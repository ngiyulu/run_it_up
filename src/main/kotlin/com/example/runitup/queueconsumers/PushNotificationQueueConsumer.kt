package com.example.runitup.queueconsumers


import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.ReceiveRequest
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class PushNotificationQueueConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope
): BaseQueueConsumer(queueService, appScope) {




    private suspend fun processJob(body: String) {
        // Your custom processing logic here
        log.info("Processing job: $body")

        // Example: simulate work
    }

    override suspend fun processData() {
        // Fetch up to 5 messages from the "jobs" queue
        val batch = queueService.receiveMessages(
            ReceiveRequest(queue = "jobs", maxNumberOfMessages = 5, waitSeconds = 10)
        )


        if (batch.messages.isEmpty()) {
            log.debug("No messages in 'jobs' queue.")
            return
        }

        log.info("Fetched ${batch.messages.size} messages from 'jobs' queue")

        coroutineScope {
            for (msg in batch.messages) {
                launch {
                    try {
                        processJob(msg.body)
                        queueService.deleteMessage("jobs", msg.receiptHandle)
                        log.info("Processed and deleted message ${msg.messageId}")
                    } catch (t: Throwable) {
                        log.error("Failed to process message ${msg.messageId}", t)
                    }
                }
            }
        }
    }
}
