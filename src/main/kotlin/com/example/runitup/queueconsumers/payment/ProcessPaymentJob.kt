package com.example.runitup.queueconsumers.payment


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class ProcessPaymentJob(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val bookingDbService: BookingDbService,
    private val cacheManager: MyCacheManager,
    private val bookingPricingAdjuster: BookingPricingAdjuster
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.RUN_PROCESS_PAYMENT, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("PaymentFailedConsumer is running")
        val data: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        val runSessionId = data.payload
        withContext(Dispatchers.IO) {
            val run = cacheManager.getRunSession(runSessionId)
            if(run == null){
                return@withContext
            }
            if(run.status != RunStatus.CONFIRMED){
                return@withContext
            }
            if(run.isSessionFree()){
                return@withContext
            }
            val booking  = bookingDbService.getBookingList(runSessionId)
            //TODO: we have to test how failure will work
            booking.forEach {
                bookingPricingAdjuster.captureBookingHolds(
                    it.id.orEmpty(),
                    it.userId,
                    it.customerId.orEmpty(),
                    "usd",
                    it.currentTotalCents
                )
            }
        }
    }
}

data class PaymentFailedModel(val bookingId:String, val runSessionId:String)
