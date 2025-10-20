package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.QueueMessage
import com.example.runitup.mobile.queue.RedisKeys
import com.example.runitup.mobile.rest.v1.restcontroller.QueueOverview
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

@Service
class LightSqsService(
    private val redis: StringRedisTemplate,
    private val appScope: CoroutineScope,
    @Value("\${queue.defaultVisibilitySeconds:30}") private val defaultVisibility: Int,
    @Value("\${queue.defaultWaitSeconds:20}") private val defaultWaitSeconds: Int,
    @Value("\${queue.defaultMaxReceiveCount:5}") private val defaultMaxReceiveCount: Int,
    @Value("\${queue.poll.dueScanMs:1000}") private val scanMs: Long,
    @Value("\${queue.dlq.retry.enabled:true}") private val dlqRetryEnabled: Boolean = true,
    @Value("\${queue.dlq.retry.intervalMs:300000}") private val dlqRetryIntervalMs: Long = 300_000,
    @Value("\${queue.dlq.retry.limitPerDlq:50}") private val dlqRetryLimit: Int = 50
    ) {
    private val om = jacksonObjectMapper()
    private val io = Dispatchers.IO // for blocking Redis ops

    init {
        // Start async maintenance loop
        appScope.launch(CoroutineName("queue-maintenance").let { it }) {
            // runs sequentially; cheap and predictable
            while (isActive) { maintenanceOnce(); delay(scanMs) }
        }
        // existing maintenance loop stays…
        if (dlqRetryEnabled) {
            appScope.launch(CoroutineName("dlq-redrive")) {
                while (isActive) { retryAllDlqsOnce(dlqRetryLimit); delay(dlqRetryIntervalMs) }
            }
        }
    }

    /** Scan all queues, find DLQs, and redrive up to [limitPerDlq] messages each. */
    suspend fun retryAllDlqsOnce(limitPerDlq: Int = 50): Int = withContext(io) {
        val cfgKeys = scanKeys("q:*:cfg")   // was: redis.keys("q:*:cfg")
        var totalMoved = 0
        for (cfgKey in cfgKeys) {
            val qName = cfgKey.substringAfter("q:").substringBefore(":cfg")
            val cfg = redis.opsForHash<String, String>().entries(cfgKey)
            val dlqName = cfg["dlqName"].takeUnless { it.isNullOrBlank() } ?: continue
            totalMoved += retryDlqMessages(dlqName, mainQueue = qName, limit = limitPerDlq)
        }
        totalMoved
    }

    /** Move up to [limit] messages from [dlqName] back to [mainQueue]. Resets receiveCount to 0. */
    private suspend fun retryDlqMessages(dlqName: String, mainQueue: String, limit: Int): Int = withContext(io) {
        val dlqList = RedisKeys.dlq(dlqName)
        var moved = 0
        repeat(limit) {
            val msgId = redis.opsForList().rightPop(dlqList) ?: return@repeat
            val srcKey = RedisKeys.msg(dlqName, msgId)
            val json = redis.opsForValue().get(srcKey) ?: return@repeat

            // reset receiveCount so it can go through its normal retry window again
            val qm = om.readValue(json, com.example.runitup.mobile.model.QueueMessage::class.java).copy(receiveCount = 0)
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

    suspend fun sendMessage(
        queue: String,
        body: String,
        attributes: Map<String, String> = emptyMap(),
        delaySeconds: Int = 0
    ): String = withContext(io) {
        val id = UUID.randomUUID().toString()
        val msgKey = RedisKeys.msg(queue, id)
        val message = com.example.runitup.mobile.model.QueueMessage(id, body, attributes, delaySeconds, 0)
        val json = om.writeValueAsString(message)
        redis.opsForValue().set(msgKey, json)

        if (delaySeconds > 0) {
            val dueAt = Instant.now().toEpochMilli() + delaySeconds * 1000L
            redis.opsForZSet().add(RedisKeys.delayed(queue), id, dueAt.toDouble())
        } else {
            redis.opsForList().leftPush(RedisKeys.ready(queue), id)
        }
        id
    }

    suspend fun receiveMessages(req: ReceiveRequest): ReceiveResponse {
        val cfg = loadCfg(req.queue)
        val wait = req.waitSeconds ?: defaultWaitSeconds
        val visibility = (req.visibilitySeconds ?: cfg.visibilitySeconds).coerceAtLeast(1)
        val deadline = System.currentTimeMillis() + wait * 1000L
        val out = mutableListOf<ReceivedMessage>()

        while (out.size < req.maxNumberOfMessages) {
            // Try pop non-blocking
            val msgId = withContext(io) { redis.opsForList().rightPop(RedisKeys.ready(req.queue)) }
            if (msgId == null) {
                if (System.currentTimeMillis() >= deadline) break
                // non-blocking wait
                delay(120)
                continue
            }

            val msgKey = RedisKeys.msg(req.queue, msgId)
            val json = withContext(io) { redis.opsForValue().get(msgKey) } ?: continue
            val qm = om.readValue(json, com.example.runitup.mobile.model.QueueMessage::class.java)

            val updated = qm.copy(receiveCount = qm.receiveCount + 1)
            withContext(io) { redis.opsForValue().set(msgKey, om.writeValueAsString(updated)) }

            // DLQ threshold?
            if (updated.receiveCount > loadCfg(req.queue).maxReceiveCount) {
                withContext(io) { moveToDlq(req.queue, updated) }
                continue
            }

            // Lease (visibility)
            val visDeadline = System.currentTimeMillis() + visibility * 1000L
            withContext(io) {
                redis.opsForZSet().add(RedisKeys.inflight(req.queue), updated.id, visDeadline.toDouble())
            }
            val receiptHandle = UUID.randomUUID().toString()
            withContext(io) {
                redis.opsForValue().set(RedisKeys.receipt(req.queue, receiptHandle), updated.id)
            }

            out += ReceivedMessage(
                messageId = updated.id,
                body = updated.body,
                attributes = updated.attributes,
                receiptHandle = receiptHandle
            )
        }
        return ReceiveResponse(out)
    }

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

    // ---- Queue admin / stats ----

    private data class Cfg(val visibilitySeconds: Int, val maxReceiveCount: Int, val dlqName: String?)

    private suspend fun loadCfg(queue: String): Cfg = withContext(io) {
        val h = redis.opsForHash<String, String>()
        val m = h.entries(RedisKeys.cfg(queue))
        val vis = m["visibilitySeconds"]?.toIntOrNull() ?: defaultVisibility
        val max = m["maxReceiveCount"]?.toIntOrNull() ?: defaultMaxReceiveCount
        val dlq = m["dlqName"].takeUnless { it.isNullOrBlank() }
        Cfg(vis, max, dlq)
    }

    private suspend fun moveToDlq(queue: String, qm: com.example.runitup.mobile.model.QueueMessage) = withContext(io) {
        val cfg = loadCfg(queue)
        val dlq = cfg.dlqName ?: return@withContext
        val srcKey = RedisKeys.msg(queue, qm.id)
        val dstKey = RedisKeys.msg(dlq, qm.id)
        redis.opsForValue().get(srcKey)?.let { redis.opsForValue().set(dstKey, it) }
        redis.opsForList().leftPush(RedisKeys.dlq(dlq), qm.id)
        redis.opsForZSet().remove(RedisKeys.inflight(queue), qm.id)
        redis.delete(srcKey)
    }

    suspend fun listQueuesWithStats(): List<QueueOverview> = withContext(io) {
        val cfgKeys = scanKeys("q:*:cfg")   // was: redis.keys("q:*:cfg")
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

    // One maintenance tick; called in the loop in init{}
    //maintenanceOnce() function is one of the core background tasks that keeps your Redis-based queue system working correctly behind the scenes.
    //It’s a housekeeping function that runs regularly (every second, in your case) to handle two things that make a queue like AWS SQS reliable:
    //
    //Promote delayed messages → move them into the “ready” queue when their delay expires.
    //
    //Re-queue expired in-flight messages → if a worker took a message but didn’t ack/delete it before its visibility timeout, put it back so another worker can process it.
    //
    //In SQS terms, it’s doing:
    //"move messages from DelayQueue → ReadyQueue"
    //
    //"move expired leased messages → ReadyQueue"
    private suspend fun maintenanceOnce() = withContext(io) {
        val now = System.currentTimeMillis().toDouble()
        val delayedKeys = scanKeys("q:*:delayed")   // was: redis.keys("q:*:delayed")
        for (delayedKey in delayedKeys) {
            val q = delayedKey.substringAfter("q:").substringBefore(":delayed")

            // move due delayed -> ready
            val due = redis.opsForZSet().rangeByScore(delayedKey, Double.NEGATIVE_INFINITY, now) ?: emptySet()
            if (due.isNotEmpty()) {
                due.forEach { msgId ->
                    redis.opsForZSet().remove(delayedKey, msgId)
                    redis.opsForList().leftPush(RedisKeys.ready(q), msgId)
                }
            }

            // requeue expired inflight
            val inflightKey = RedisKeys.inflight(q)
            val expired = redis.opsForZSet().rangeByScore(inflightKey, Double.NEGATIVE_INFINITY, now) ?: emptySet()
            if (expired.isNotEmpty()) {
                expired.forEach { msgId ->
                    redis.opsForZSet().remove(inflightKey, msgId)
                    redis.opsForList().leftPush(RedisKeys.ready(q), msgId)
                }
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


}

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

