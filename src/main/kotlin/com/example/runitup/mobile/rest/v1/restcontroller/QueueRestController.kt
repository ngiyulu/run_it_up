package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.ReceiveRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/queues")
class QueueController(private val q: LightSqsService) {

    @PostMapping("/create")
   fun create(@RequestBody req: CreateQueueRequest) =
        kotlinx.coroutines.runBlocking{
            q.createQueue(req.name, req.visibilitySeconds, req.maxReceiveCount, "${req.name}-dlq").let { mapOf("ok" to true) }
        }


    @PostMapping("/{name}/messages")
    suspend fun send(@PathVariable name: String, @RequestBody req: SendMessageRequest) =
        kotlinx.coroutines.runBlocking {
            mapOf("messageId" to q.sendMessage(name, req.body, req.attributes ?: emptyMap(), req.delaySeconds ?: 0))
        }


    @GetMapping("/{name}/messages")
    fun receive(@PathVariable name: String, query: ReceiveQuery) =
        kotlinx.coroutines.runBlocking {
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
        kotlinx.coroutines.runBlocking {
            q.listQueuesWithStats()
        }

    @DeleteMapping("/{name}/messages")
    fun delete(@PathVariable name: String, @RequestBody req: AckRequest) =
        kotlinx.coroutines.runBlocking {
            mapOf("deleted" to q.deleteMessage(name, req.receiptHandle))
        }


    @DeleteMapping("/delete/{name}")
    fun deleteQueue(
        @PathVariable name: String,
        @RequestParam(defaultValue = "true") includeDlq: Boolean) =
        kotlinx.coroutines.runBlocking {
            q.deleteQueue(name, includeDlq)
        }

    @PostMapping("/{name}/visibility")
    fun changeVisibility(@PathVariable name: String, @RequestBody req: ChangeVisibilityRequest) =
        kotlinx.coroutines.runBlocking {
            mapOf("changed" to q.changeMessageVisibility(name, req.receiptHandle, req.visibilitySeconds))
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