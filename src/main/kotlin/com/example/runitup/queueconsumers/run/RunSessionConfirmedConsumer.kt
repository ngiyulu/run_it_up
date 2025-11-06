package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.util.*

@Component
class RunSessionConfirmedConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val bookingDbService: BookingDbService,
    private val runSessionRepository: RunSessionRepository,
    private val runSessionService: RunSessionService
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.RUN_CONFIRMATION_JOB, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("RunSessionConfirmedConsumer is running")
        val jobData: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        val payload = jobData.payload
        withContext(Dispatchers.IO) {
            val runSessionDb = runSessionRepository.findById(payload)
            val run = runSessionDb.get()
            val runSessionId = run.id.orEmpty()
            if(run.status != RunStatus.PENDING){
                logger.info("Run $payload already ${run.status}, skipping")
                return@withContext
            }
            val booking = bookingDbService.getBookingList(runSessionId)
            val joinedCount = booking.size
            if (joinedCount < run.minimumPlayer) {
                logger.info("Run $runSessionId not eligible: $joinedCount/${run.minimumPlayer}")
                return@withContext
            }
            // this is  mechanism for making sure that there is no race condition issue
            val result = runSessionService.confirmRunSession(runSessionId)
            // We "won" the race — send notifications now
            if(result.modifiedCount == 1L){
                if(!run.isSessionFree()){
                    val data = JobEnvelope(
                        jobId = UUID.randomUUID().toString(),
                        taskType = "Process payment after confirmation from admin",
                        payload = run.id.orEmpty()
                    )
                    queueService.sendJob(QueueNames.RUN_PROCESS_PAYMENT, data)
                }
               complete(booking, run)
                logger.info("Run $runSessionId confirmed and notifications sent (${joinedCount})")
            }
            else{
                // Someone else updated the run concurrently (confirmed/cancelled)
                logger.info("Run $runSessionId confirmation lost race (modifiedCount=0); skipping")
            }

        }
    }

    private fun complete(booking:List<Booking>, runSession: RunSession){
        runSessionService.notifyUsers(booking, runSession)
        runSession.status = RunStatus.CONFIRMED
        runSessionRepository.save(runSession)
    }
}
