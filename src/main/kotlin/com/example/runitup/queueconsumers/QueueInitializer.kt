package com.example.runitup.queueconsumers

import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.service.LightSqsService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration

@Configuration
class QueueInitializer(
    private val queueService: LightSqsService
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        log.info("Initializing Redis queues...")

        runBlocking {
            listOf(
                QueueInitializerModel(QueueNames.WAIT_LIST_JOB,  visibilitySeconds = 300),
                QueueInitializerModel(QueueNames.JOINED_RUN_JOB),
                QueueInitializerModel(QueueNames.FIRST_SESSION_JOB),
                QueueInitializerModel(QueueNames.LOCATION_JOB),
                QueueInitializerModel(QueueNames.NEW_USER_JOB),
                QueueInitializerModel(QueueNames.RUN_SESSION_PUSH_JOB),
                QueueInitializerModel(QueueNames.RUN_CANCELLED_JOB),
                QueueInitializerModel(QueueNames.RUN_PROCESS_PAYMENT),
                QueueInitializerModel(QueueNames.RUN_CONFIRMATION_JOB, visibilitySeconds = 300),
                QueueInitializerModel(QueueNames.PAYMENT_FAILED_JOB)

            ).forEach { queue ->
                queueService.createQueue(queue.queue, 30, 5)
                log.info("✅ Queue created: $queue and its DLQ")
            }
        }
        log.info("All queues initialized successfully.")
    }
}

data class QueueInitializerModel(val queue: String, val visibilitySeconds: Int = 30)