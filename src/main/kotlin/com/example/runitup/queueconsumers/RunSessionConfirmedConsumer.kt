package com.example.runitup.queueconsumers


import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.controllers.runsession.CoordinateUpdateModel
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class RunSessionConfirmedConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val runSessionPushNotificationService: RunSessionPushNotificationService,
    private val runSessionRepository: RunSessionRepository
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.RUN_CONFIRMATION, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        log.info("RunSessionConfirmedConsumer is running")
        val data: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        val payload = data.payload
        withContext(Dispatchers.IO) {
            val runSessionDb = runSessionRepository.findById(payload)
            val run = runSessionDb.get()
            run.bookingList.forEach {
                runSessionPushNotificationService.runSessionConfirmed(it.userId, run)
            }
        }
    }
}
