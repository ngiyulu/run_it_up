// queueconsumers/BaseQueueConsumer.kt
package com.example.runitup.queueconsumers

import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.ReceiveRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import javax.annotation.PostConstruct

abstract class BaseQueueConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val tracker: JobTrackerService,
    private val queueName: String,
    protected val om: ObjectMapper
) {
    protected val log = LoggerFactory.getLogger(javaClass)
    private val workerId: String = "${javaClass.simpleName}-${java.net.InetAddress.getLocalHost().hostName}"

    @PostConstruct
    fun startPolling() {
        appScope.launch(CoroutineName(coroutineName())) {
            while (isActive) {
                try {
                    pollOnce()
                } catch (e: Exception) {
                    log.error("Error during queue poll", e)
                }
                delay(delay())
            }
        }
    }

    private suspend fun pollOnce() {
        val batch = queueService.receiveMessages(
            ReceiveRequest(queue = queueName, maxNumberOfMessages = maxNumberOfMessages(), waitSeconds = waitSeconds())
        )
        if (batch.messages.isEmpty()) {
            log.debug("No messages in '$queueName'.")
            return
        }
        log.info("Fetched ${batch.messages.size} messages from '$queueName'")

        coroutineScope {
            for (msg in batch.messages) {
                launch {
                    // read receiveCount out of Redis message if you store it there, or decode from attributes if you add it
                    val receiveCount = msg.attributes["receiveCount"]?.toIntOrNull() ?: 1

                    // Try to parse into JobEnvelope; if not, treat raw body as payload
                    var jobId = msg.messageId
                    var taskType = "UNKNOWN"
                    var attempt = 1
                    var traceId: String? = null

                    try {
                        val env: JobEnvelope<Map<String, Any?>> = om.readValue(msg.body)
                        jobId = env.jobId
                        taskType = env.taskType
                        attempt = env.attempt
                        traceId = env.traceId
                        MDC.put("traceId", traceId)
                    } catch (_: Exception) {
                        // keep defaults; still track raw
                    }

                    val exec = tracker.start(
                        jobId = jobId,
                        queue = queueName,
                        taskType = taskType,
                        workerId = workerId,
                        attempt = attempt,
                        receiveCount = receiveCount,
                        traceId = traceId
                    )

                    try {
                        // Let subclass do actual work
                        processOne(msg.body, taskType, jobId, traceId)

                        // Ack on success
                        queueService.deleteMessage(queueName, msg.receiptHandle)
                        tracker.success(exec.id!!)
                        log.info("Processed and deleted message $jobId")
                    } catch (t: Throwable) {
                        tracker.failure(exec.id!!, t)
                        log.error("Failed to process message $jobId", t)
                        // Do not delete → message will become visible after visibility timeout and retry until DLQ
                    } finally {
                        MDC.clear()
                    }
                }
            }
        }
    }

    protected abstract suspend fun processOne(
        rawBody: String,
        taskType: String,
        jobId: String,
        traceId: String?
    )

    private fun coroutineName() = this::class::simpleName.name

    fun delay(): Long = 30_000

    fun waitSeconds(): Int = 10

    fun maxNumberOfMessages(): Int = 2
}
