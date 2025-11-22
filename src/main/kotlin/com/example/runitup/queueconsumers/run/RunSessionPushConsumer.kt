package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.USER_ID
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.dto.PushJobModel
import com.example.runitup.mobile.rest.v1.dto.PushJobType
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class RunSessionPushConsumer(
    queueService: LightSqsService,
    appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val cacheManager: MyCacheManager,
    private val runSessionPushNotificationService: RunSessionPushNotificationService,
    private val bookingRepository: BookingRepository
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.RUN_SESSION_PUSH_JOB, objectMapper) {

    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("RunSessionPushConsumer is running")
        val jobData: JobEnvelope<PushJobModel> = objectMapper.readValue(rawBody) as JobEnvelope<PushJobModel>
        val payload = jobData.payload
        withContext(Dispatchers.IO) {
            logger.info("payload = $payload")
            when(payload.type){
                PushJobType.CONFIRM_RUN -> notifyConfirmationRun(payload.dataId)
                PushJobType.CANCEL_RUN -> notifyCancelledRun(payload.dataId)
                PushJobType.USER_JOINED -> notifyUserJoined(payload.dataId, payload.metadata[USER_ID]?: "", payload.metadata[AppConstant.BOOKING_ID]?: System.currentTimeMillis().toString())
                PushJobType.USER_JOINED_WAITLIST -> notifyUserJoinedWaitList(payload.dataId, payload.metadata[USER_ID]?: "", payload.metadata[AppConstant.BOOKING_ID]?: System.currentTimeMillis().toString())
                PushJobType.BOOKING_UPDATED -> notifyUserBookingUpdated(payload.dataId, payload.metadata[USER_ID]?: "", payload.metadata[AppConstant.BOOKING_ID]?: System.currentTimeMillis().toString())
                PushJobType.BOOKING_CANCELLED_BY_ADMIN -> notifyUserBookingCancelledByAdmin(payload.dataId)
                PushJobType.BOOKING_CANCELLED_BY_USER -> notifyUserBookingCancelledByUser(payload.dataId)

            }
        }
    }

    private fun notifyConfirmationRun(runId:String){
        val run = getRunSession(runId)
        if(run.status != RunStatus.CONFIRMED){
            return
        }
        val booking = bookingRepository.findByRunSessionIdAndStatusIn(
            runId,
            mutableListOf(BookingStatus.JOINED)
        )
        booking.forEach {
            runSessionPushNotificationService.runSessionConfirmed(it.userId, run)
        }

    }

    private fun  notifyCancelledRun(runId:String){
        val run = getRunSession(runId)
        if(run.status != RunStatus.CANCELLED){
            return
        }
        val booking = bookingRepository.findByRunSessionIdAndStatusIn(
            runId,
            mutableListOf(BookingStatus.JOINED)
        )
        val runSessionCreator = userRepository.findByLinkedAdmin(run.hostedBy.orEmpty())

        runSessionPushNotificationService.runSessionCancelled(run, booking, runSessionCreator)
    }


    private fun  notifyUserJoined(runId:String, userId:String, bookingId: String){
        val run = getRunSession(runId)
        if(run.status == RunStatus.CANCELLED ||
            run.status == RunStatus.COMPLETED ||
            run.status == RunStatus.PROCESSED){
            return
        }
        logger.info("notifyUserJoined userId = $userId")
        run.hostedBy?.let {
            val user = getUser(userId)
            val adminUser = getAdmin(it)
            if(adminUser.id != userId){
                runSessionPushNotificationService.userJoinedRunSession(adminUser.id.orEmpty(), user, run, bookingId)
            }
            else{
                logger.info("user who joined is also the admin")
            }

        }?: run {
            logger.error("run.hostedBy parameter is null, runId = $runId")
        }
    }

    private fun  notifyUserJoinedWaitList (runId:String, userId:String, bookingId: String){
        val run = getRunSession(runId)
        if(run.status == RunStatus.CANCELLED ||
            run.status == RunStatus.COMPLETED ||
            run.status == RunStatus.PROCESSED){
            return
        }
        logger.info("notifyUserJoined waitlist userId = $userId")
        run.hostedBy?.let {
            val user = getUser(userId)
            val adminUser = getAdmin(it)
            if(adminUser.id != userId){
                runSessionPushNotificationService.userJoinedWaitListRunSession(adminUser.id.orEmpty(), user, run, bookingId)
            }
            else{
                logger.info("user who joined is also the admin")
            }

        }?: run {
            logger.error("run.hostedBy parameter is null, runId = $runId")
        }
    }


    private fun  notifyUserBookingUpdated(runId:String, userId:String, bookingId:String){
        val run = getRunSession(runId)
        if(run.status == RunStatus.CANCELLED ||
            run.status == RunStatus.COMPLETED ||
            run.status == RunStatus.PROCESSED){
            return
        }
        logger.info("notifyUserBookingUpdated userId = $userId")
        run.hostedBy?.let {
            val user = getUser(userId)
            val adminUser = getAdmin(it)
            if(adminUser.id != userId){
                runSessionPushNotificationService.userUpdatedBooking(adminUser.id.orEmpty(), user, run, bookingId)
            }
            else{
                logger.info("user who joined is also the admin")
            }

        }?: run {
            logger.error("run.hostedBy parameter is null, runId = $runId")
        }
    }

    private fun  notifyUserBookingCancelledByAdmin(bookingId:String){
        val booking = getBooking(bookingId)
        if(booking.status != BookingStatus.CANCELLED){
            return
        }
        val run = getRunSession(booking.runSessionId)
        runSessionPushNotificationService.runSessionBookingCancelledByAdmin(booking.userId, run, bookingId)
    }

    private fun  notifyUserBookingCancelledByUser(bookingId:String){
        val booking = getBooking(bookingId)
        if(booking.status != BookingStatus.CANCELLED){
            return
        }
        val run = getRunSession(booking.runSessionId)
        logger.info("notifyUserBookingCancelledByUser userId = ${booking.user}")
        run.hostedBy?.let {
            val user = getUser(booking.userId)
            val adminUser = getAdmin(it)
            if(adminUser.id != booking.userId){
                runSessionPushNotificationService.runSessionBookingCancelledByUser(adminUser.id.orEmpty(), run,  user, bookingId)
            }
            else{
                logger.info("user who cancelled booking is also the admin")
            }
        }?: run {
            logger.error("run.hostedBy parameter is null, runId = ${run.id.orEmpty()}")
        }
    }



    private fun getRunSession(runId:String):RunSession{
        return  cacheManager.getRunSession(runId)?: throw ApiRequestException("run session not found")
    }

    private fun getUser(userId:String):User{
        return  cacheManager.getUser(userId)?: throw ApiRequestException("user not found")
    }

    private fun getAdmin(userId:String):User{
        return  userRepository.findByLinkedAdmin(userId)?: throw ApiRequestException("user not found")
    }

    private fun getBooking(bookingId:String):Booking{
        val booking = bookingRepository.findById(bookingId)
        if(!booking.isPresent){
            throw  ApiRequestException("booking not found")
        }
        return  booking.get()
    }
}
