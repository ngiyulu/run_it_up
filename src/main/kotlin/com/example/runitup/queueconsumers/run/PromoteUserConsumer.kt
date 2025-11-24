package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.PromotionService
import com.example.runitup.mobile.service.RunSessionEventLogger
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class PromoteUserConsumer(
    queueService: LightSqsService,
    appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val runSessionEventLogger: RunSessionEventLogger,
    private val objectMapper: ObjectMapper,
    private val promotionService: PromotionService
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.WAIT_LIST_JOB, objectMapper) {

    override fun delay(): Long = 2_000          // poll every 2s
    override fun waitSeconds(): Int = 5         // short poll but responsive
    override fun maxNumberOfMessages(): Int = 10

    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?, receiptHandle:String) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("PromoteUserConsumer, is running")
        val env: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        queueService.changeMessageVisibility(
            QueueNames.WAIT_LIST_JOB,
            receiptHandle,
            120   // give the worker 2 minutes
        )
        logger.info(env.toString())
        runSessionEventLogger.log(
            sessionId = env.jobId,
            action = RunSessionAction.USER_PROMOTED_FROM_WAITLIST,
            actor = Actor(ActorType.SYSTEM, jobId),
            newStatus = null,
            reason = "System attempting to promote user from waitlist",
            correlationId = null,
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        promotionService.promoteNextWaitlistedUser(env.payload)
    }
}
