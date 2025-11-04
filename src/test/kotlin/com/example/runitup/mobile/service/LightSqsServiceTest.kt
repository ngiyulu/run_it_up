// src/test/kotlin/com/example/runitup/mobile/service/LightSqsServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.JobEnvelope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.*
import java.time.Instant

class LightSqsServiceTest {

    private val redis = mockk<StringRedisTemplate>()
    private val valueOps = mockk<ValueOperations<String, String>>(relaxed = true)
    private val listOps = mockk<ListOperations<String, String>>(relaxed = true)
    private val zsetOps = mockk<ZSetOperations<String, String>>(relaxed = true)
    private val hashOps = mockk<HashOperations<String, String, String>>(relaxed = true)

    // stop background loops
    private val deadScope = CoroutineScope(Job().apply { cancel() })

    // IMPORTANT: Jackson with Kotlin + JavaTime
    private val om = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val defaultVisibility = 30
    private val defaultWaitSeconds = 20
    private val defaultMaxReceiveCount = 5
    private val scanMs = 1000L

    private lateinit var service: LightSqsService

    @BeforeEach
    fun setUp() {
        every { redis.opsForValue() } returns valueOps
        every { redis.opsForList() } returns listOps
        every { redis.opsForZSet() } returns zsetOps
        every { redis.opsForHash<String, String>() } returns hashOps

        every { hashOps.entries(any()) } returns mapOf(
            "visibilitySeconds" to defaultVisibility.toString(),
            "maxReceiveCount" to defaultMaxReceiveCount.toString(),
            "dlqName" to ""
        )

        service = LightSqsService(
            redis = redis,
            appScope = deadScope,
            defaultVisibility = defaultVisibility,
            defaultWaitSeconds = defaultWaitSeconds,
            defaultMaxReceiveCount = defaultMaxReceiveCount,
            scanMs = scanMs,
            dlqRetryEnabled = false,
            dlqRetryIntervalMs = 300_000,
            dlqRetryLimit = 50,
            om = om
        )
    }

    @AfterEach fun tearDown() { clearAllMocks(); unmockkAll() }

    // ---------- sendJob (immediate)
    @Test
    fun `sendJob immediate enqueues to ready and writes body`() = runTest {
        val queue = "orders"
        val env = JobEnvelope("job-1", "PROCESS_ORDER", TestPayload("p-1"), traceId = "tr-1", createdAtMs = Instant.now())

        val setKey = slot<String>()
        val setVal = slot<String>()
        every { valueOps.set(capture(setKey), capture(setVal)) } just Runs

        val readyKey = slot<String>()
        val pushedId = slot<String>()
        every { listOps.leftPush(capture(readyKey), capture(pushedId)) } returns 1L

        val id = service.sendJob(queue, env, attributes = mapOf("k" to "v"), delaySeconds = 0)

        assertThat(id).isEqualTo(pushedId.captured)
        val qm = om.readValue(setVal.captured, QueueMessage::class.java)
        assertThat(qm.id).isEqualTo(id)
        assertThat(qm.attributes["k"]).isEqualTo("v")
        assertThat(readyKey.captured).contains(":ready")
        verify(exactly = 0) { zsetOps.add(match { it.contains(":delayed") }, any(), any()) }
    }

    // ---------- sendJob (delayed)
    @Test
    fun `sendJob with delay schedules in delayed zset`() = runTest {
        val queue = "emails"
        val env = JobEnvelope("job-2", "SEND_EMAIL", TestPayload("x"), traceId = "t", createdAtMs = Instant.now())

        every { valueOps.set(any<String>(), any()) } just Runs

        val delayedKey = slot<String>()
        val delayedId = slot<String>()
        val score = slot<Double>()
        every { zsetOps.add(capture(delayedKey), capture(delayedId), capture(score)) } returns true
        every { listOps.leftPush(any<String>(), any<String>()) } returns 0L

        val id = service.sendJob(queue, env, delaySeconds = 5)

        assertThat(delayedKey.captured).contains(":delayed")
        assertThat(delayedId.captured).isEqualTo(id)
        assertThat(score.captured).isGreaterThan(Instant.now().toEpochMilli().toDouble())
        verify(exactly = 0) { listOps.leftPush(match { it.contains(":ready") }, any<String>()) }
    }

    // ---------- receiveJobBatch
    @Test
    fun `receiveJobBatch pops, increments, inflight & receipt, deserializes`() = runTest {
        val queue = "photos"
        val msgId = "m-123"
        val env = JobEnvelope("job-77", "PROCESS_PHOTO", TestPayload("img-7"), traceId = "tr-77", createdAtMs = Instant.now())
        val stored = QueueMessage(msgId, om.writeValueAsString(env), mapOf("k1" to "v1"), delaySeconds = 0, receiveCount = 0)

        // pop from ready
        every { listOps.rightPop(match { it.contains(":ready") }) } returns msgId

        // fetch stored doc
        val getKey = slot<String>()
        every { valueOps.get(capture(getKey)) } returns om.writeValueAsString(stored)

        // set updated doc (target ONLY :msg: keys)
        val msgSetKey = slot<String>()
        val msgSetVal = slot<String>()
        every { valueOps.set(match<String> { it.contains(":msg:") }, capture(msgSetVal)) } answers {
            msgSetKey.captured = firstArg()
            Unit
        }

        // save receipt (target ONLY :receipt: keys)
        val receiptSetKey = slot<String>()
        val receiptSetVal = slot<String>()
        every { valueOps.set(match<String> { it.contains(":receipt:") }, capture(receiptSetVal)) } answers {
            receiptSetKey.captured = firstArg()
            Unit
        }

        // inflight add
        val inflightKey = slot<String>()
        val inflightId = slot<String>()
        val inflightScore = slot<Double>()
        every { zsetOps.add(capture(inflightKey), capture(inflightId), capture(inflightScore)) } returns true

        // cfg lookups
        every { hashOps.entries(match { it.contains(":cfg") }) } returns mapOf(
            "visibilitySeconds" to defaultVisibility.toString(),
            "maxReceiveCount" to defaultMaxReceiveCount.toString(),
            "dlqName" to ""
        )

        val res: JobReceiveResponse<TestPayload> =
            service.receiveJobBatch(ReceiveRequest(queue, maxNumberOfMessages = 1, waitSeconds = 0), TestPayload::class.java)

        // assertions
        assertThat(res.jobs).hasSize(1)
        val r = res.jobs.first()
        assertThat(r.messageId).isEqualTo(msgId)
        assertThat(r.attributes["k1"]).isEqualTo("v1")
        assertThat(r.envelope.taskType).isEqualTo("PROCESS_PHOTO")
        assertThat(r.envelope.payload.id).isEqualTo("img-7")
        assertThat(r.receiptHandle).isNotBlank()

        // updated doc receiveCount incremented
        val updated = om.readValue(msgSetVal.captured, QueueMessage::class.java)
        assertThat(updated.receiveCount).isEqualTo(1)
        assertThat(msgSetKey.captured).contains(":msg:")

        // inflight and receipt
        assertThat(inflightKey.captured).contains(":inflight")
        assertThat(inflightId.captured).isEqualTo(msgId)
        assertThat(inflightScore.captured).isGreaterThan(Instant.now().toEpochMilli().toDouble())
        assertThat(receiptSetKey.captured).contains(":receipt:")
        assertThat(receiptSetVal.captured).isEqualTo(msgId)
    }

    // ---------- delete / visibility

    @Test
    fun `deleteMessage removes receipt, inflight, and message doc`() = runTest {
        val queue = "docs"
        val handle = "rh-1"
        val msgId = "m-9"

        every { valueOps.get(match<String> { it.contains(":receipt:") }) } returns msgId
        every { redis.delete(match<String> { it.contains(":receipt:") }) } returns true
        every { redis.delete(match<String> { it.contains(":msg:") }) } returns true
        every { zsetOps.remove(match<String> { it.contains(":inflight") }, msgId) } returns 1L

        val ok = service.deleteMessage(queue, handle)
        assertThat(ok).isTrue()

        verify { zsetOps.remove(match<String> { it.contains(":inflight") }, msgId) }
        verify { redis.delete(match<String> { it.contains(":msg:") }) }
        verify { redis.delete(match<String> { it.contains(":receipt:") }) }
    }

    @Test
    fun `changeMessageVisibility updates inflight deadline`() = runTest {
        val queue = "tasks"
        val handle = "rh-2"
        val msgId = "m-22"

        every { valueOps.get(match<String> { it.contains(":receipt:") }) } returns msgId

        val inflightKey = slot<String>()
        val idSlot = slot<String>()
        val score = slot<Double>()
        every { zsetOps.add(capture(inflightKey), capture(idSlot), capture(score)) } returns true

        val ok = service.changeMessageVisibility(queue, handle, 42)
        assertThat(ok).isTrue()
        assertThat(inflightKey.captured).contains(":inflight")
        assertThat(idSlot.captured).isEqualTo(msgId)
        assertThat(score.captured).isGreaterThan(Instant.now().toEpochMilli().toDouble())
    }

    data class TestPayload(val id: String)
}
