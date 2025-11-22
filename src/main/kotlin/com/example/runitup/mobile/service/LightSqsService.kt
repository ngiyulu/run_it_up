package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.RedisKeys
import com.example.runitup.mobile.rest.v1.restcontroller.QueueOverview
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

// ---------- Typed (new) ----------


data class PeekMessage(
    val id: String,
    val receiveCount: Int,
    val attributes: Map<String, String>,
    val createdAtMs: Long?,     // if present in envelope
    val taskType: String?,      // if present in envelope
    val traceId: String?,       // if present in envelope
    val visibilityDeadlineMs: Long? = null,  // inflight only (when it will reappear)
    val dueAtMs: Long? = null,               // delayed only (when it will move to ready)
)
data class JobReceiveResponse<T>(
    val jobs: List<ReceivedJob<T>>
)

data class ReceivedJob<T>(
    val messageId: String,
    val envelope: JobEnvelope<T>,
    val attributes: Map<String, String>,
    val receiptHandle: String
)

// ---------- Legacy (kept for back-compat) ----------
data class ReceiveRequest(
    val queue: String,
    val maxNumberOfMessages: Int = 1,
    val waitSeconds: Int? = null,
    val visibilitySeconds: Int? = null
)

data class ReceiveResponse(
    val messages: List<ReceivedMessage>
)

data class ReceivedMessage(
    val messageId: String,
    val body: String,
    val attributes: Map<String, String>,
    val receiptHandle: String
)

// ---------- Internal storage shape ----------
data class QueueMessage(
    val id: String = "",
    val body: String = "",                       // JSON: JobEnvelope<*>
    val attributes: Map<String, String> = emptyMap(),
    val delaySeconds: Int = 0,
    val receiveCount: Int = 0
)

/*
defaultVisibility: This is the visibility timeout — how long (in seconds) a message stays invisible to other consumers after it’s received, before it’s automatically made visible again if not deleted.
 */

/*
defaultWaitSeconds: This is the long-poll duration — how long the consumer (receiver) waits for a message if none are immediately available.
Instead of returning instantly with “no messages,” the queue can wait a few seconds to see if new jobs arrive, reducing empty polling loops.
 */

/*
defaultMaxReceiveCount: This is the retry limit — the maximum number of times a single message can be received before it’s sent to the Dead Letter Queue (DLQ).
Every time a message is delivered to a consumer (and not deleted), its receiveCount increases.
If it’s retried too many times (e.g. due to repeated crashes), it’s moved to a DLQ for manual review
If defaultMaxReceiveCount = 5:

The message can fail up to 5 times

On the 6th attempt → automatically moved to DLQ
 */


/*
scanMs: This is the interval (milliseconds) for the background maintenance loop that scans Redis and performs housekeeping tasks.
The maintenanceOnce() coroutine periodically checks:

If any delayed jobs are due → move them to the “ready” queue.

If any inflight jobs (being processed) have expired → requeue them (because their visibility expired).
 */

/*
dlqRetryIntervalMs: This sets the interval (in milliseconds) for how often the DLQ redrive process runs — that is, how frequently your system will scan all DLQs and move eligible messages back to their main queues for reprocessing.
if dlqRetryIntervalMs = 300,000 ms = 5 minutes
So by default, every 5 minutes, the system will:

Look through all configured queues.

Find their DLQs.

Attempt to redrive (requeue) up to 50 jobs from each DLQ
back into the main processing queue.
 */

/*
states
1. Ready
List
q:<queue>:ready
Available for consumers to receive immediately



2. Inflight
ZSet (sorted set)
q:<queue>:inflight
Currently being processed by a worker; hidden from others


3.
Delayed
ZSet
q:<queue>:delayed
Scheduled to become ready at a future time


DLQ (Dead Letter Queue)
List
q:<queue>-dlq:dlq
Failed too many times (maxReceiveCount exceeded)


 */


@Service
class LightSqsService(
    @Qualifier("queueRedisTemplate")
    private val redis: StringRedisTemplate,
    private val appScope: CoroutineScope,
    @Value("\${queue.defaultVisibilitySeconds:90}") private val defaultVisibility: Int,
    @Value("\${queue.defaultWaitSeconds:20}") private val defaultWaitSeconds: Int,
    @Value("\${queue.defaultMaxReceiveCount:5}") private val defaultMaxReceiveCount: Int,
    @Value("\${queue.poll.dueScanMs:1000}") private val scanMs: Long,
    @Value("\${queue.dlq.retry.enabled:true}") private val dlqRetryEnabled: Boolean = true,
    @Value("\${queue.dlq.retry.intervalMs:300000}") private val dlqRetryIntervalMs: Long = 300_000,
    @Value("\${queue.dlq.retry.limitPerDlq:50}") private val dlqRetryLimit: Int = 50,
    protected val om: ObjectMapper

) {
    private val io = Dispatchers.IO

    init {
        appScope.launch(CoroutineName("queue-maintenance")) {
            while (isActive) { maintenanceOnce(); delay(scanMs) }
        }
        if (dlqRetryEnabled) {
            appScope.launch(CoroutineName("dlq-redrive")) {
                while (isActive) { retryAllDlqsOnce(dlqRetryLimit); delay(dlqRetryIntervalMs) }
            }
        }
    }

    // -----------------------
    // Producer: enqueue jobs
    // -----------------------

    /** Enqueue a typed JobEnvelope<T>. Returns messageId. */
    suspend fun <T> sendJob(
        queue: String,
        envelope: JobEnvelope<T>,
        attributes: Map<String, String> = emptyMap(),
        delaySeconds: Int = 0
    ): String = withContext(io) {
        val id = UUID.randomUUID().toString()
        val msgKey = RedisKeys.msg(queue, id)
        val bodyJson = om.writeValueAsString(envelope)
        val qm = QueueMessage(id, bodyJson, attributes, delaySeconds, 0)
        redis.opsForValue().set(msgKey, om.writeValueAsString(qm))

        if (delaySeconds > 0) {
            val dueAt = Instant.now().toEpochMilli() + delaySeconds * 1000L
            redis.opsForZSet().add(RedisKeys.delayed(queue), id, dueAt.toDouble())
        } else {
            redis.opsForList().leftPush(RedisKeys.ready(queue), id)
        }
        id
    }

    /** Back-compat: accept raw string; stored as JobEnvelope<JsonNode>. */
    suspend fun sendMessage(
        queue: String,
        body: String,
        attributes: Map<String, String> = emptyMap(),
        delaySeconds: Int = 0
    ): String {
        val env = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "RAW_STRING",
            payload = om.readTree(body) as JsonNode,
            traceId = UUID.randomUUID().toString(),
            createdAtMs = Instant.now()
        )
        return sendJob(queue, env, attributes, delaySeconds)
    }

    // -----------------------
    // Consumer: receive jobs
    // -----------------------

    /** Receive jobs and deserialize to JobEnvelope<T> with payloadClass. */
    suspend fun <T> receiveJobBatch(
        req: ReceiveRequest,
        payloadClass: Class<T>
    ): JobReceiveResponse<T> {
        val cfg = loadCfg(req.queue)
        val wait = req.waitSeconds ?: defaultWaitSeconds
        val visibility = (req.visibilitySeconds ?: cfg.visibilitySeconds).coerceAtLeast(1)
        val deadline = System.currentTimeMillis() + wait * 1000L
        val out = mutableListOf<ReceivedJob<T>>()

        while (out.size < req.maxNumberOfMessages) {
            val msgId = withContext(io) { redis.opsForList().rightPop(RedisKeys.ready(req.queue)) }
            if (msgId == null) {
                if (System.currentTimeMillis() >= deadline) break
                delay(120); continue
            }

            val msgKey = RedisKeys.msg(req.queue, msgId)
            val json = withContext(io) { redis.opsForValue().get(msgKey) } ?: continue
            val qm = om.readValue(json, QueueMessage::class.java)

            val updated = qm.copy(receiveCount = qm.receiveCount + 1)
            withContext(io) { redis.opsForValue().set(msgKey, om.writeValueAsString(updated)) }

            if (updated.receiveCount > loadCfg(req.queue).maxReceiveCount) {
                withContext(io) { moveToDlq(req.queue, updated) }
                continue
            }

            val visDeadline = System.currentTimeMillis() + visibility * 1000L
            withContext(io) { redis.opsForZSet().add(RedisKeys.inflight(req.queue), updated.id, visDeadline.toDouble()) }
            val receiptHandle = UUID.randomUUID().toString()
            withContext(io) { redis.opsForValue().set(RedisKeys.receipt(req.queue, receiptHandle), updated.id) }

            // ---- FIX: build JavaType, then readValue with explicit target type ----
            val envType: JavaType =
                om.typeFactory.constructParametricType(JobEnvelope::class.java, payloadClass)

            @Suppress("UNCHECKED_CAST")
            val env: JobEnvelope<T> =
                om.readValue(updated.body, envType) as JobEnvelope<T>
            // ---------------------------------------------

            out += ReceivedJob(
                messageId = updated.id,
                envelope = env,
                attributes = updated.attributes,
                receiptHandle = receiptHandle
            )
        }
        return JobReceiveResponse(out)
    }

    /** Reified convenience (must be final for inline). */
    final suspend inline fun <reified T> receiveJobBatch(req: ReceiveRequest): JobReceiveResponse<T> =
        receiveJobBatch(req, T::class.java)

    /** Back-compat: return legacy string-body response. */
    suspend fun receiveMessages(req: ReceiveRequest): ReceiveResponse {
        val r: JobReceiveResponse<JsonNode> = receiveJobBatch(req, JsonNode::class.java)
        val msgs = r.jobs.map {
            ReceivedMessage(
                messageId = it.messageId,
                body = om.writeValueAsString(it.envelope), // whole envelope as JSON string
                attributes = it.attributes,
                receiptHandle = it.receiptHandle
            )
        }
        return ReceiveResponse(msgs)
    }

    // -----------------------
    // Ack / visibility
    // -----------------------

    suspend fun deleteMessage(queue: String, receiptHandle: String): Boolean = withContext(io) {
        val receiptKey = RedisKeys.receipt(queue, receiptHandle)
        val msgId = redis.opsForValue().get(receiptKey) ?: return@withContext false
        redis.delete(receiptKey)
        redis.opsForZSet().remove(RedisKeys.inflight(queue), msgId)
        redis.delete(RedisKeys.msg(queue, msgId))
        true
    }

    suspend fun changeMessageVisibility(queue: String, receiptHandle: String, visibilitySeconds: Int): Boolean =
        withContext(io) {
            val msgId = redis.opsForValue().get(RedisKeys.receipt(queue, receiptHandle)) ?: return@withContext false
            val newDeadline = System.currentTimeMillis() + visibilitySeconds * 1000L
            redis.opsForZSet().add(RedisKeys.inflight(queue), msgId, newDeadline.toDouble())
            true
        }

    // -----------------------
    // DLQ / admin / stats
    // -----------------------


    private suspend fun moveToDlq(queue: String, qm: QueueMessage) = withContext(io) {
        val cfg = loadCfg(queue)
        val dlq = cfg.dlqName ?: return@withContext
        val srcKey = RedisKeys.msg(queue, qm.id)
        val dstKey = RedisKeys.msg(dlq, qm.id)
        redis.opsForValue().get(srcKey)?.let { redis.opsForValue().set(dstKey, it) }
        redis.opsForList().leftPush(RedisKeys.dlq(dlq), qm.id)
        redis.opsForZSet().remove(RedisKeys.inflight(queue), qm.id)
        redis.delete(srcKey)
    }
    private data class Cfg(val visibilitySeconds: Int, val maxReceiveCount: Int, val dlqName: String?)

    private suspend fun loadCfg(queue: String): Cfg = withContext(io) {
        val h = redis.opsForHash<String, String>()
        val m = h.entries(RedisKeys.cfg(queue))
        val vis = m["visibilitySeconds"]?.toIntOrNull() ?: defaultVisibility
        val max = m["maxReceiveCount"]?.toIntOrNull() ?: defaultMaxReceiveCount
        val dlq = m["dlqName"].takeUnless { it.isNullOrBlank() }
        Cfg(vis, max, dlq)
    }

    suspend fun retryAllDlqsOnce(limitPerDlq: Int = 50): Int = withContext(io) {
        val cfgKeys = scanKeys("q:*:cfg")
        var totalMoved = 0
        for (cfgKey in cfgKeys) {
            val qName = cfgKey.substringAfter("q:").substringBefore(":cfg")
            val cfg = redis.opsForHash<String, String>().entries(cfgKey)
            val dlqName = cfg["dlqName"].takeUnless { it.isNullOrBlank() } ?: continue
            totalMoved += retryDlqMessages(dlqName, mainQueue = qName, limit = limitPerDlq)
        }
        totalMoved
    }

    private suspend fun retryDlqMessages(dlqName: String, mainQueue: String, limit: Int): Int = withContext(io) {
        val dlqList = RedisKeys.dlq(dlqName)
        var moved = 0
        repeat(limit) {
            val msgId = redis.opsForList().rightPop(dlqList) ?: return@repeat
            val srcKey = RedisKeys.msg(dlqName, msgId)
            val json = redis.opsForValue().get(srcKey) ?: return@repeat

            val qm = om.readValue(json, QueueMessage::class.java).copy(receiveCount = 0)
            val updatedJson = om.writeValueAsString(qm)

            redis.opsForValue().set(RedisKeys.msg(mainQueue, msgId), updatedJson)
            redis.opsForList().leftPush(RedisKeys.ready(mainQueue), msgId)
            redis.delete(srcKey)
            moved++
        }
        moved
    }

    suspend fun createQueue(
        name: String,
        visibilitySeconds: Int? = null,
        maxReceiveCount: Int? = null,
        dlqName: String? = "${name}-dlq"
    ) = withContext(io) {
        val cfg = RedisKeys.cfg(name)
        val ops = redis.opsForHash<String, String>()
        if (!redis.hasKey(cfg)) {
            ops.put(cfg, "visibilitySeconds", (visibilitySeconds ?: defaultVisibility).toString())
            ops.put(cfg, "maxReceiveCount", (maxReceiveCount ?: defaultMaxReceiveCount).toString())
            ops.put(cfg, "dlqName", dlqName ?: "")
        }
        if (!dlqName.isNullOrBlank() && !redis.hasKey(RedisKeys.cfg(dlqName))) {
            ops.put(RedisKeys.cfg(dlqName), "visibilitySeconds", (visibilitySeconds ?: defaultVisibility).toString())
            ops.put(RedisKeys.cfg(dlqName), "maxReceiveCount", (maxReceiveCount ?: defaultMaxReceiveCount).toString())
            ops.put(RedisKeys.cfg(dlqName), "dlqName", "")
        }
    }

    suspend fun listQueuesWithStats(): List<QueueOverview> = withContext(io) {
        val cfgKeys = scanKeys("q:*:cfg")
        val listOps = redis.opsForList()
        val zsetOps = redis.opsForZSet()

        cfgKeys.map { cfgKey ->
            val name = cfgKey.substringAfter("q:").substringBefore(":cfg")
            val cfg = redis.opsForHash<String, String>().entries(cfgKey)
            val dlqName = cfg["dlqName"].takeUnless { it.isNullOrBlank() }

            QueueOverview(
                name = name,
                ready = listOps.size(RedisKeys.ready(name)) ?: 0L,
                delayed = zsetOps.size(RedisKeys.delayed(name)) ?: 0L,
                inflight = zsetOps.size(RedisKeys.inflight(name)) ?: 0L,
                dlqDepth = dlqName?.let { listOps.size(RedisKeys.dlq(it)) ?: 0L } ?: 0L,
                visibilitySeconds = cfg["visibilitySeconds"]?.toIntOrNull() ?: defaultVisibility,
                maxReceiveCount = cfg["maxReceiveCount"]?.toIntOrNull() ?: defaultMaxReceiveCount,
                dlqName = dlqName
            )
        }.sortedBy { it.name }
    }

    // ---- Maintenance ----

    private suspend fun maintenanceOnce() = withContext(io) {
        val now = System.currentTimeMillis().toDouble()

        // 1) delayed -> ready
        for (delayedKey in scanKeys("q:*:delayed")) {
            val q = delayedKey.substringAfter("q:").substringBefore(":delayed")
            val due = redis.opsForZSet().rangeByScore(delayedKey, Double.NEGATIVE_INFINITY, now).orEmpty()
            due.forEach { msgId ->
                redis.opsForZSet().remove(delayedKey, msgId)
                redis.opsForList().leftPush(RedisKeys.ready(q), msgId)
            }
        }

        // 2) expired inflight -> ready  (scan all inflight keys)
        for (inflightKey in scanKeys("q:*:inflight")) {
            val q = inflightKey.substringAfter("q:").substringBefore(":inflight")
            val expired = redis.opsForZSet().rangeByScore(inflightKey, Double.NEGATIVE_INFINITY, now).orEmpty()
            expired.forEach { msgId ->
                redis.opsForZSet().remove(inflightKey, msgId)
                redis.opsForList().leftPush(RedisKeys.ready(q), msgId)
            }
        }
    }

    /** Non-blocking key scan. */
    private suspend fun scanKeys(pattern: String, count: Long = 1000): Set<String> = withContext(io) {
        val out = mutableSetOf<String>()
        redis.execute { connection ->
            val opts = ScanOptions.scanOptions().match(pattern).count(count).build()
            connection.scan(opts).use { cursor ->
                while (cursor.hasNext()) {
                    val raw: ByteArray = cursor.next()
                    out += String(raw, StandardCharsets.UTF_8)
                }
            }
            null
        }
        out
    }

    /** Delete many keys matched by pattern using SCAN in batches (no KEYS). */
    private suspend fun deleteByPattern(pattern: String, scanCount: Long = 1000, batch: Int = 500): Long = withContext(io) {
        var deleted = 0L
        redis.execute { connection ->
            val opts = ScanOptions.scanOptions().match(pattern).count(scanCount).build()
            connection.scan(opts).use { cursor ->
                val buf = ArrayList<ByteArray>(batch)
                while (cursor.hasNext()) {
                    buf += cursor.next()
                    if (buf.size >= batch) {
                        deleted += connection.keyCommands().del(*buf.toTypedArray())!!
                        buf.clear()
                    }
                }
                if (buf.isNotEmpty()) {
                    deleted += connection.keyCommands().del(*buf.toTypedArray())!!
                }
            }
            null
        }
        deleted
    }

    suspend fun deleteQueue(name: String, includeDlq: Boolean): Map<String, Any> = withContext(io) {
        val deleted = deleteByPattern("q:$name:*")   // was: keys + delete(keys)

        var dlqDeleted = 0L
        if (includeDlq) {
            val cfg = redis.opsForHash<String, String>().entries(RedisKeys.cfg(name))
            val dlq = cfg["dlqName"].takeUnless { it.isNullOrBlank() }
            if (dlq != null) {
                dlqDeleted = deleteByPattern("q:$dlq:*")
            }
        }
        mapOf("queue" to name, "deletedKeys" to deleted, "deletedDlqKeys" to dlqDeleted)
    }


    private fun ensureQueueExists(queue: String) {
        val cfgKey = RedisKeys.cfg(queue)
        if (!redis.hasKey(cfgKey)) {
            throw IllegalArgumentException("Queue '$queue' does not exist")
        }
    }
    suspend fun peekReady(queue: String, limit: Long = 50): List<PeekMessage> = withContext(io) {
        // Right end is the "oldest" in your flow; for peeking we can show newest first
        ensureQueueExists(queue)
        val ids = redis.opsForList().range(RedisKeys.ready(queue), 0, limit - 1) ?: emptyList()
        ids.mapNotNull { id -> loadPeek(queue, id) }
    }

    suspend fun peekInflight(queue: String, limit: Long = 50): List<PeekMessage> = withContext(io) {
        ensureQueueExists(queue)
        val withScores = redis.opsForZSet()
            .rangeWithScores(RedisKeys.inflight(queue), 0, limit - 1)
            ?.toList()
            ?: emptyList()
        withScores.mapNotNull { tuple ->
            val id = tuple.value as String
            val deadlineMs = tuple.score?.toLong()
            loadPeek(queue, id)?.copy(visibilityDeadlineMs = deadlineMs)
        }
    }

    suspend fun peekDelayed(queue: String, limit: Long = 50): List<PeekMessage> = withContext(io) {
        ensureQueueExists(queue)
        val withScores = redis.opsForZSet()
            .rangeWithScores(RedisKeys.delayed(queue), 0, limit - 1)
            ?.toList()
            ?: emptyList()
        withScores.mapNotNull { tuple ->
            val id = tuple.value as String
            val dueAtMs = tuple.score?.toLong()
            loadPeek(queue, id)?.copy(dueAtMs = dueAtMs)
        }
    }

    suspend fun peekDlq(queue: String, limit: Long = 50): List<PeekMessage> = withContext(io) {
        ensureQueueExists(queue)
        val cfg = redis.opsForHash<String, String>().entries(RedisKeys.cfg(queue))
        val dlq = cfg["dlqName"].takeUnless { it.isNullOrBlank() } ?: return@withContext emptyList()
        val ids = redis.opsForList().range(RedisKeys.dlq(dlq), 0, limit - 1) ?: emptyList()
        ids.mapNotNull { id -> loadPeek(dlq, id) } // note: messages live under the DLQ's namespace
    }

    /** Load QueueMessage and extract a small envelope preview without deserializing payload T. */
    private fun loadPeek(ns: String, id: String): PeekMessage? {
        val json = redis.opsForValue().get(RedisKeys.msg(ns, id)) ?: return null
        val qm = om.readValue(json, QueueMessage::class.java)

        // Try to pull a few top-level envelope fields for quick visibility
        val envNode = om.readTree(qm.body)
        val createdAtMs = envNode.get("createdAtMs")?.asLong()
        val taskType = envNode.get("taskType")?.asText()
        val traceId = envNode.get("traceId")?.asText()

        return PeekMessage(
            id = qm.id,
            receiveCount = qm.receiveCount,
            attributes = qm.attributes,
            createdAtMs = createdAtMs,
            taskType = taskType,
            traceId = traceId
        )
    }
}
