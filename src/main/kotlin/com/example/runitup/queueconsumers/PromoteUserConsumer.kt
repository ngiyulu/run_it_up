package com.example.runitup.queueconsumers


import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import kotlinx.coroutines.*
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.context.annotation.Configuration

@Configuration
class PromoteUserConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.JOB_WAIT_LIST) {




    private suspend fun processJob(body: String) {
        // Your custom processing logic here
        log.info("Processing job: $body")

        // Example: simulate work
    }

    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        log.info("PromoteUserConsumer, is running")
        val env: JobEnvelope<String> = om.readValue(rawBody) as JobEnvelope<String>



    }


}
