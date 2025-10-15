package com.example.runitup.service

import com.example.runitup.model.QueueMessage
import com.example.runitup.queue.RedisKeys
import com.example.runitup.web.rest.v1.restcontroller.QueueOverview
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class LightSqsService(
    private val redis: StringRedisTemplate,
    @Value("\${queue.defaultVisibilitySeconds:30}") private val defaultVisibility: Int,
    @Value("\${queue.defaultWaitSeconds:20}") private val defaultWaitSeconds: Int,
    @Value("\${queue.defaultMaxReceiveCount:5}") private val defaultMaxReceiveCount: Int
) {
    private val om = jacksonObjectMapper()

    fun createQueue(
        name: String,
        visibilitySeconds: Int? = null,
        maxReceiveCount: Int? = null,
        dlqName: String? = "${name}-dlq"
    ) {
        val cfg = RedisKeys.cfg(name)
        val ops = redis.opsForHash<String, String>()
        if (!redis.hasKey(cfg)) {
            ops.put(cfg, "visibilitySeconds", (visibilitySeconds ?: defaultVisibility).toString())
            ops.put(cfg, "maxReceiveCount", (maxReceiveCount ?: defaultMaxReceiveCount).toString())
            ops.put(cfg, "dlqName", dlqName ?: "")
        }
        // ensure DLQ cfg exists too
        if (!dlqName.isNullOrBlank() && !redis.hasKey(RedisKeys.cfg(dlqName))) {
            ops.put(RedisKeys.cfg(dlqName), "visibilitySeconds", (visibilitySeconds ?: defaultVisibility).toString())
            ops.put(RedisKeys.cfg(dlqName), "maxReceiveCount", (maxReceiveCount ?: defaultMaxReceiveCount).toString())
            ops.put(RedisKeys.cfg(dlqName), "dlqName", "") // DLQ of DLQ = none
        }
    }

    fun sendMessage(queue: String, body: String, attributes: Map<String,String> = emptyMap(), delaySeconds: Int = 0): String {
        val id = UUID.randomUUID().toString()
        val msgKey = RedisKeys.msg(queue, id)
        val message = QueueMessage(id, body, attributes, delaySeconds, 0)
        val json = om.writeValueAsString(message)
        redis.opsForValue().set(msgKey, json)

        if (delaySeconds > 0) {
            val dueAt = Instant.now().toEpochMilli() + delaySeconds * 1000L
            redis.opsForZSet().add(RedisKeys.delayed(queue), id, dueAt.toDouble())
        } else {
            redis.opsForList().leftPush(RedisKeys.ready(queue), id)
        }
        return id
    }

    fun receiveMessages(req: ReceiveRequest): ReceiveResponse {
        val cfg = loadCfg(req.queue)
        val wait = req.waitSeconds ?: defaultWaitSeconds
        val visibility = (req.visibilitySeconds ?: cfg.visibilitySeconds).coerceAtLeast(1)
        val now = Instant.now().toEpochMilli()
        val until = now + wait * 1000L
        val out = mutableListOf<ReceivedMessage>()

        while (out.size < req.maxNumberOfMessages) {
            // try to pop
            val msgId = redis.opsForList().rightPop(RedisKeys.ready(req.queue))
            if (msgId == null) {
                // long poll: small sleep until timeout
                if (Instant.now().toEpochMilli() >= until) break
                Thread.sleep(150)
                continue
            }
            val msgKey = RedisKeys.msg(req.queue, msgId)
            val json = redis.opsForValue().get(msgKey) ?: continue
            val qm = om.readValue(json, QueueMessage::class.java).copy(receiveCount = 0) // will overwrite below

            // bump receive count
            val updated = qm.copy(receiveCount = qm.receiveCount + 1)
            redis.opsForValue().set(msgKey, om.writeValueAsString(updated))

            // check DLQ threshold
            if (updated.receiveCount > loadCfg(req.queue).maxReceiveCount) {
                moveToDlq(req.queue, updated)
                continue
            }

            // lease (visibility)
            val deadline = Instant.now().toEpochMilli() + visibility * 1000L
            redis.opsForZSet().add(RedisKeys.inflight(req.queue), updated.id, deadline.toDouble())
            val receiptHandle = UUID.randomUUID().toString()
            redis.opsForValue().set(RedisKeys.receipt(req.queue, receiptHandle), updated.id)

            out += ReceivedMessage(
                messageId = updated.id,
                body = updated.body,
                attributes = updated.attributes,
                receiptHandle = receiptHandle
            )
        }
        return ReceiveResponse(out)
    }

    fun deleteMessage(queue: String, receiptHandle: String): Boolean {
        val receiptKey = RedisKeys.receipt(queue, receiptHandle)
        val msgId = redis.opsForValue().get(receiptKey) ?: return false
        redis.delete(receiptKey)
        // remove from inflight and delete payload
        redis.opsForZSet().remove(RedisKeys.inflight(queue), msgId)
        redis.delete(RedisKeys.msg(queue, msgId))
        return true
    }

    fun deleteQueue(name: String, includeDlq: Boolean = true): Map<String, Any> {
        val pattern = "q:$name:*"
        val keys = redis.keys(pattern) ?: emptySet()

        val deletedCount = if (keys.isNotEmpty()) redis.delete(keys) else 0

        var dlqDeletedCount = 0L
        if (includeDlq) {
            // find and delete DLQ if configured
            val cfg = redis.opsForHash<String, String>().entries(RedisKeys.cfg(name))
            val dlqName = cfg["dlqName"].takeUnless { it.isNullOrBlank() }
            if (dlqName != null) {
                val dlqKeys = redis.keys("q:$dlqName:*") ?: emptySet()
                if (dlqKeys.isNotEmpty()) {
                    dlqDeletedCount = redis.delete(dlqKeys)
                }
            }
        }

        return mapOf(
            "queue" to name,
            "deletedKeys" to deletedCount,
            "deletedDlqKeys" to dlqDeletedCount
        )
    }

    fun changeMessageVisibility(queue: String, receiptHandle: String, visibilitySeconds: Int): Boolean {
        val msgId = redis.opsForValue().get(RedisKeys.receipt(queue, receiptHandle)) ?: return false
        val newDeadline = Instant.now().toEpochMilli() + visibilitySeconds * 1000L
        redis.opsForZSet().add(RedisKeys.inflight(queue), msgId, newDeadline.toDouble())
        return true
    }

    fun listQueues(): List<Map<String, String>> {
        val keys = redis.keys("q:*:cfg") ?: return emptyList()
        val ops = redis.opsForHash<String, String>()
        return keys.mapNotNull { key ->
            val name = key.substringAfter("q:").substringBefore(":cfg")
            val cfg = ops.entries(key)
            mapOf(
                "name" to name,
                "visibilitySeconds" to (cfg["visibilitySeconds"] ?: ""),
                "maxReceiveCount" to (cfg["maxReceiveCount"] ?: ""),
                "dlqName" to (cfg["dlqName"] ?: "")
            )
        }
    }

    fun listQueuesWithStats(): List<QueueOverview> {
        // NOTE: KEYS is fine for small numbers of queues. For thousands, switch to SCAN.
        val cfgKeys = redis.keys("q:*:cfg") ?: emptySet()
        if (cfgKeys.isEmpty()) return emptyList()

        val h = redis.opsForHash<String, String>()
        val listOps = redis.opsForList()
        val zsetOps = redis.opsForZSet()

        return cfgKeys.map { cfgKey ->
            val name = cfgKey.substringAfter("q:").substringBefore(":cfg")
            val cfg = h.entries(cfgKey)
            val visibility = cfg["visibilitySeconds"]?.toIntOrNull() ?: defaultVisibility
            val maxReceive = cfg["maxReceiveCount"]?.toIntOrNull() ?: defaultMaxReceiveCount
            val dlqName = cfg["dlqName"].takeUnless { it.isNullOrBlank() }

            val ready = listOps.size(RedisKeys.ready(name)) ?: 0L
            val delayed = zsetOps.size(RedisKeys.delayed(name)) ?: 0L
            val inflight = zsetOps.size(RedisKeys.inflight(name)) ?: 0L
            val dlqDepth = dlqName?.let { listOps.size(RedisKeys.dlq(it)) ?: 0L } ?: 0L

            QueueOverview(
                name = name,
                ready = ready,
                delayed = delayed,
                inflight = inflight,
                dlqDepth = dlqDepth,
                visibilitySeconds = visibility,
                maxReceiveCount = maxReceive,
                dlqName = dlqName
            )
        }.sortedBy { it.name }
    }

    private data class Cfg(val visibilitySeconds: Int, val maxReceiveCount: Int, val dlqName: String?)
    private fun loadCfg(queue: String): Cfg {
        val h = redis.opsForHash<String, String>()
        val m = h.entries(RedisKeys.cfg(queue))
        val vis = m["visibilitySeconds"]?.toIntOrNull() ?: defaultVisibility
        val max = m["maxReceiveCount"]?.toIntOrNull() ?: defaultMaxReceiveCount
        val dlq = m["dlqName"].takeUnless { it.isNullOrBlank() }
        return Cfg(vis, max, dlq)
    }

    private fun moveToDlq(queue: String, qm: QueueMessage) {
        val cfg = loadCfg(queue)
        val dlq = cfg.dlqName ?: return
        // push ID into DLQ list and keep payload under DLQ namespace
        val srcKey = RedisKeys.msg(queue, qm.id)
        val dstKey = RedisKeys.msg(dlq, qm.id)
        redis.opsForValue().get(srcKey)?.let { redis.opsForValue().set(dstKey, it) }
        redis.opsForList().leftPush(RedisKeys.dlq(dlq), qm.id)
        // cleanup
        redis.opsForZSet().remove(RedisKeys.inflight(queue), qm.id)
        redis.delete(srcKey)
    }

    /** Promote due delayed msgs; re-queue expired inflight (visibility timeout). */
    @Scheduled(fixedDelayString = "\${queue.poll.dueScanMs:1000}")
    fun maintenance() {
        val now = Instant.now().toEpochMilli().toDouble()
        // For simplicity scan all queues by key pattern (small deployments).
        // In production, keep a registry of queues.
        val keys = redis.keys("q:*:delayed") ?: emptySet()
        for (delayedKey in keys) {
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
                    // back to ready (will increase receiveCount on next delivery)
                    redis.opsForList().leftPush(RedisKeys.ready(q), msgId)
                }
            }
        }
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

