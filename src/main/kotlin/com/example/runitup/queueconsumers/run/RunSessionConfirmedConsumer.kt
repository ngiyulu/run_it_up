package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.runsession.JoinSessionQueueModel
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionEventLogger
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.*

@Component
class RunSessionConfirmedConsumer(
    queueService: LightSqsService,
    appScope: CoroutineScope,
    trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val runSessionEventLogger: RunSessionEventLogger,
    private val bookingDbService: BookingDbService,
    private val cacheManager: MyCacheManager,
    private val runSessionService: RunSessionService,
    private val runSesionPushNotificationService: RunSessionPushNotificationService
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.RUN_CONFIRMATION_JOB, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?, receiptHandle:String) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("RunSessionConfirmedConsumer is running")
        val jobData: JobEnvelope<JoinSessionQueueModel> = objectMapper.readValue(rawBody) as JobEnvelope<JoinSessionQueueModel>
        val payload = jobData.payload
        withContext(Dispatchers.IO) {
            val run = cacheManager.getRunSession(payload.runSessionId)
            if(run == null){
                logger.error("error, run ${payload.runSessionId} was not found")
                 return@withContext
            }
            val runSessionId = run.id.orEmpty()
            if(run.status != RunStatus.PENDING){
                logger.info("Run $payload already ${run.status}, skipping")
                return@withContext
            }
            val booking = bookingDbService.getJoinedBookingList(runSessionId)
            val joinedCount = booking.size
            if (joinedCount < run.minimumPlayer) {
                logger.info("Run $runSessionId not eligible: $joinedCount/${run.minimumPlayer}")
                return@withContext
            }
            // this is  mechanism for making sure that there is no race condition issue
            val result = runSessionService.confirmRunSession(runSessionId)
            logger.info(result.toString())
            // We "won" the race — send notifications now
            if(result.modifiedCount == 1L){
                if(!run.isSessionFree()){
                    val data = JobEnvelope(
                        jobId = UUID.randomUUID().toString(),
                        taskType = "Process payment after confirmation from admin",
                        payload = run.id.orEmpty()
                    )
                    queueService.sendJob(QueueNames.RUN_PROCESS_PAYMENT, data,  delaySeconds = 0)
                }
               complete(run, jobId)
                logger.info("Run $runSessionId confirmed and notifications sent (${joinedCount})")
            }
            else{
                // Someone else updated the run concurrently (confirmed/cancelled)
                logger.info("Run $runSessionId confirmation lost race (modifiedCount=0); skipping")
            }

        }
    }

    private fun complete(runSession: RunSession, jobId: String){
        val runSessionCreator = userRepository.findByLinkedAdmin(runSession.hostedBy.orEmpty())
        runSessionCreator?.let {
            runSesionPushNotificationService.runSessionConfirmed(it.id.orEmpty(), runSession)
        }
        runSession.status = RunStatus.CONFIRMED
        runSessionEventLogger.log(
            sessionId = runSession.id.orEmpty(),
            action = RunSessionAction.SESSION_CONFIRMED,
            actor = Actor(ActorType.SYSTEM, jobId),
            newStatus = null,
            reason = "System confirming session",
            correlationId = null,
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        cacheManager.updateRunSession(runSession)
    }
}
