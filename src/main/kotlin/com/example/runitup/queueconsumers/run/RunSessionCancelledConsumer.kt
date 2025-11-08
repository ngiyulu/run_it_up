package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RefundReasonCode
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.payment.RefundService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class RunSessionCancelledConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val  bookingPricingAdjuster: BookingPricingAdjuster,
    private val bookingRepository: BookingRepository,
    private val paymentStateRepository: BookingPaymentStateRepository,
    private val refundService: RefundService,
    private val cacheManager: MyCacheManager,
    private val runSessionPushNotificationService: RunSessionPushNotificationService,
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.RUN_CANCELLED_JOB, objectMapper) {
    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("RunSessionCancelledConsumer is running")
        val data: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        val payload = data.payload
        withContext(Dispatchers.IO) {
            val run = cacheManager.getRunSession(payload) ?: return@withContext
            if(!run.isSessionFree()){
                // this means payment were processed so we need a refund
                if(run.statusBeforeCancel == RunStatus.ONGOING || run.statusBeforeCancel == RunStatus.CONFIRMED){
                    refund(run)
                }
                else if(run.status == RunStatus.PENDING){
                    cancelHold(run)
                }
            }
            run.bookingList.forEach {
                runSessionPushNotificationService.runSessionCancelled(it.userId, run)
            }
        }
    }

    private fun cancelHold(runSession: RunSession){
        val bookingList = bookingRepository.findByRunSessionIdAndStatusIn(runSession.id.orEmpty(), mutableListOf(BookingStatus.JOINED))
        bookingList.forEach { b ->
            bookingPricingAdjuster.cancelAuthorizationAndUpdate(
                b.id.orEmpty(),
                b.userId,
                b.customerId.orEmpty(),
                "usd",
                b.paymentId.orEmpty()
            )
        }
    }

    private fun refund(runSession: RunSession){
        val bookingList = bookingRepository.findByRunSessionIdAndStatusIn(runSession.id.orEmpty(), mutableListOf(BookingStatus.JOINED))
        bookingList.forEach { b ->
            val paymentState = paymentStateRepository.findByBookingId(b.id.orEmpty())
            if(paymentState != null){
                refundService.refundPaymentIntent(
                    b.id.orEmpty(),
                    b.userId,
                    b.customerId.orEmpty(),
                    "userId",
                    b.paymentId.orEmpty(),
                    paymentState.totalCapturedCents,
                    RefundReasonCode.HOST_CANCELLED,
                )
            }

        }

    }
}
