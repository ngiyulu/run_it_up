//package com.example.runitup.queueconsumers
//
//
//import com.example.runitup.mobile.queue.QueueNames
//import com.example.runitup.mobile.service.JobTrackerService
//import com.example.runitup.mobile.service.LightSqsService
//import com.example.runitup.mobile.service.ReceiveRequest
//import kotlinx.coroutines.*
//import org.slf4j.LoggerFactory
//import org.springframework.context.annotation.Configuration
//import javax.annotation.PostConstruct
//
//@Configuration
//class PushNotificationQueueConsumer(
//    private val queueService: LightSqsService,
//    private val appScope: CoroutineScope,
//    private val trackerService: JobTrackerService
//): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.JOB_WAIT_LIST) {
//
//
//
//
//    private suspend fun processJob(body: String) {
//        // Your custom processing logic here
//        log.info("Processing job: $body")
//
//        // Example: simulate work
//    }
//
//    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
//        // Fetch up to 5 messages from the "jobs" queue
//        val env: JobEnvelope<PushPayload> = om.readValue(rawBody)
//
//        if (batch.messages.isEmpty()) {
//            log.debug("No messages in 'jobs' queue.")
//            return
//        }
//
//        log.info("Fetched ${batch.messages.size} messages from 'jobs' queue")
//
//        coroutineScope {
//            for (msg in batch.messages) {
//                launch {
//                    try {
//                        processJob(msg.body)
//                        queueService.deleteMessage("jobs", msg.receiptHandle)
//                        log.info("Processed and deleted message ${msg.messageId}")
//                    } catch (t: Throwable) {
//                        log.error("Failed to process message ${msg.messageId}", t)
//                    }
//                }
//            }
//        }
//    }
//
//
//}
