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
class PaymentFailedConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val runSessionRepository: RunSessionRepository,
    private val bookingPaymentStateRepository: BookingPaymentStateRepository,
    private val smsService: ClickSendSmsService,
    private val cacheManager: MyCacheManager
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.PAYMENT_FAILED_JOB, objectMapper) {


    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("PaymentFailedConsumer is running")
        val data: JobEnvelope<PaymentFailedModel> = objectMapper.readValue(rawBody) as JobEnvelope<PaymentFailedModel>
        val payload = data.payload
        withContext(Dispatchers.IO) {
            val runDb = runSessionRepository.findById(payload.runSessionId)
            val run = runDb.get()
            val bookingPayment = bookingPaymentStateRepository.findByBookingId(payload.bookingId)
            if(bookingPayment != null){
                val user = cacheManager.getUser(bookingPayment?.userId.orEmpty())
                if(user != null){
                    val message = "run ${run.title} taking place on ${run.date} with id ${run.id} has a failed payment for ${user.getFullName()} "
                    //smsService.sendSmsDetailed("", message)
                }
                else{
                    throw Error("user is null $payload")
                }

            }
            else{
                throw Error("booking payment  is null $payload")
            }


        }

    }

}

data class PaymentFailedModel(val bookingId:String, val runSessionId:String)
