package com.example.runitup.queueconsumers


import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.PromotionService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component

@Component
class PromoteUserConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val promotionService: PromotionService
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.WAIT_LIST_JOB, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("PromoteUserConsumer, is running")
        val env: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        promotionService.promoteNextWaitlistedUser(env.jobId)
    }
}
