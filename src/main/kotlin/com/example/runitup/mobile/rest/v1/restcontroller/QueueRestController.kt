package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.rest.v1.dto.PushJobModel
import com.example.runitup.mobile.rest.v1.dto.PushJobType
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.ReceiveRequest
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/queues")
@Profile(value = ["dev", "local"]) // ✅ active for both dev and local profiles
class QueueController(private val q: LightSqsService) {

    @PostMapping("/create")
   fun create(@RequestBody req: CreateQueueRequest) =
        runBlocking{
            q.createQueue(req.name, req.visibilitySeconds, req.maxReceiveCount, "${req.name}-dlq").let { mapOf("ok" to true) }
        }


    @PostMapping("/{name}/messages")
    suspend fun send(@PathVariable name: String, @RequestBody req: SendMessageRequest) =
        runBlocking {
            mapOf("messageId" to q.sendMessage(name, req.body, req.attributes ?: emptyMap(), req.delaySeconds ?: 0))
        }


    @GetMapping("/{name}/messages")
    fun receive(@PathVariable name: String, query: ReceiveQuery) =
        runBlocking {
            q.receiveMessages(
                ReceiveRequest(
                    queue = name,
                    maxNumberOfMessages = query.maxNumberOfMessages ?: 1,
                    waitSeconds = query.waitSeconds,
                    visibilitySeconds = query.visibilitySeconds
                )
            )
        }


    @GetMapping("/list/stats")
    fun listQueuesWithStats() =
        runBlocking {
            q.listQueuesWithStats()
        }

    @DeleteMapping("/{name}/messages")
    fun delete(@PathVariable name: String, @RequestBody req: AckRequest) =
        runBlocking {
            mapOf("deleted" to q.deleteMessage(name, req.receiptHandle))
        }



    @PostMapping("/add/waitlist")
    fun addToQueue(@RequestBody req: WaitListQueueMessage) =
        runBlocking {
            val jobEnvelope = JobEnvelope(
                jobId = UUID.randomUUID().toString(),
                taskType = "Notification booking cancelled by user",
                payload = req.runSession
            )
            q.sendJob(QueueNames.WAIT_LIST_JOB, jobEnvelope,  delaySeconds = 0)
        }


    @DeleteMapping("/delete/{name}")
    fun deleteQueue(
        @PathVariable name: String,
        @RequestParam(defaultValue = "true") includeDlq: Boolean) =
        runBlocking {
            q.deleteQueue(name, includeDlq)
        }

    @DeleteMapping("/clear/{name}")
    fun clearQueue(
        @PathVariable name: String,
        @RequestParam(defaultValue = "true") includeDlq: Boolean) =
        runBlocking {
            q.clearQueue(name, includeDlq)
        }

    @PostMapping("/{name}/visibility")
    fun changeVisibility(@PathVariable name: String, @RequestBody req: ChangeVisibilityRequest) =
        runBlocking {
            mapOf("changed" to q.changeMessageVisibility(name, req.receiptHandle, req.visibilitySeconds))
        }

    /** Peek items in a queue section without consuming them. */
    @GetMapping("/{name}/peek")
    fun peek(
        @PathVariable name: String,
        @RequestParam(defaultValue = "ready") section: String, // ready|inflight|delayed|dlq
        @RequestParam(defaultValue = "50") limit: Long
    ) = runBlocking {
        when (section.lowercase()) {
            "ready"    -> q.peekReady(name, limit)
            "inflight" -> q.peekInflight(name, limit)
            "delayed"  -> q.peekDelayed(name, limit)
            "dlq"      -> q.peekDlq(name, limit)
            else       -> error("Unknown section '$section'. Use ready|inflight|delayed|dlq.")
        }
    }

}

data class CreateQueueRequest(
    val name: String,
    val visibilitySeconds: Int? = null,
    val maxReceiveCount: Int? = null,
)
data class SendMessageRequest(
    val body: String,
    val attributes: Map<String,String>? = null,
    val delaySeconds: Int? = 0
)
data class ReceiveQuery(
    val maxNumberOfMessages: Int? = 1,
    val waitSeconds: Int? = null,
    val visibilitySeconds: Int? = null
)

data class QueueOverview(
    val name: String,
    val ready: Long,
    val delayed: Long,
    val inflight: Long,
    val dlqDepth: Long,
    val visibilitySeconds: Int,
    val maxReceiveCount: Int,
    val dlqName: String?
)
data class AckRequest(val receiptHandle: String)
data class ChangeVisibilityRequest(val receiptHandle: String, val visibilitySeconds: Int)

data class WaitListQueueMessage(val queue:String, val runSession:String)