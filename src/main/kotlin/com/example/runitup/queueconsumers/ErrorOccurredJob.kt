package com.example.runitup.queueconsumers


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.service.ClickSendSmsService
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.TextService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class ErrorOccurredJob(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val smsService: ClickSendSmsService,
    private val cacheManager: MyCacheManager
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.PAYMENT_FAILED_JOB, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("ErrorOccurredJob is running")
        val data: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        val payload = data.payload
        withContext(Dispatchers.IO) {
            smsService.sendSmsDetailed("", "error happened $payload")
        }
    }

}

