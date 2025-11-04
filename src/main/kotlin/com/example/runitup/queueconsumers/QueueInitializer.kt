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
                QueueNames.WAIT_LIST_JOB,
                QueueNames.JOINED_RUN_JOB,
                QueueNames.FIRST_SESSION_JOB,
                QueueNames.LOCATION_JOB,
                QueueNames.NEW_USER_JOB
            ).forEach { queue ->
                queueService.createQueue(queue, 30, 5)
                log.info("✅ Queue created: $queue and its DLQ")
            }
        }

        log.info("All queues initialized successfully.")
    }
}