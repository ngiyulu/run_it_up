package com.example.runitup.mobile.service

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.web.dto.Role
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.DeleteParticipantFromConversationModel
import com.stripe.param.PaymentIntentCancelParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
// user decides not to participate anymore
class LeaveSessionService {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository


    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var waitListPaymentService: WaitListPaymentService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    @Autowired
    lateinit var cacheManager: MyCacheManager

    @Autowired
    lateinit var textService: TextService

    @Autowired
    lateinit var messagingService: MessagingService

    @Autowired
    lateinit var bookingPricingAdjuster: BookingPricingAdjuster

    @Autowired
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var queueService: LightSqsService


     fun execute(request: CancelSessionModel, user:User, admin:AdminUser? = null): RunSession {
         val locale = LocaleContextHolder.getLocale().toString()
         val runDb = runSessionRepository.findById(request.sessionId)
         val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException(textService.getText("user_not_found", LocaleContextHolder.getLocale().toString()))
         if(!runDb.isPresent){
            throw ApiRequestException(textService.getText("invalid_session_id",locale ))
        }

        val run = runDb.get()
        if(!run.isDeletable()){
            throw  ApiRequestException(textService.getText("invalid_session_cancel", locale))
        }
         if(admin != null){
             if(admin.role != Role.SUPER_ADMIN && run.hostedBy != admin.id){
                 throw ApiRequestException(textService.getText("unauthorized_user", locale))
             }
         }
        val booking: Booking = bookingDbService.getBooking(user.id.orEmpty(), run.id.orEmpty())
            ?: throw  ApiRequestException(textService.getText("invalid_params", locale))
        // this means a hold payment was created so we have to cancel it
         if(!run.isSessionFree()){
             if(run.status != RunStatus.PENDING){
                 val res = bookingPricingAdjuster.cancelAuthorizationAndUpdate(
                     bookingId = booking.id.orEmpty(),
                     userId = user.id.orEmpty(),
                     customerId = user.stripeId.orEmpty(),
                     currency = "us",
                     paymentIntentId = booking.paymentId.orEmpty(),
                     reason = PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER // optional
                 )
                 if (!res.ok) {
                     throw ApiRequestException("payment_error")
                 }
                 cancelWaitListPayment(booking)
             }
         }

         run.bookingList.removeAll {
            it.userId == user.id.orEmpty()
         }
         run.bookings.removeAll {
             it.userId == user.id.orEmpty()
         }
         run.waitList.removeAll {
             it.userId == user.id.orEmpty()
         }

         booking.status =  BookingStatus.CANCELLED
         bookingDbService.bookingRepository.save(booking)
         messagingService.removeParticipant(DeleteParticipantFromConversationModel(user.id.orEmpty(), run.id.orEmpty())).block()
         completeFlow(run)
         return runSessionService.updateRunSession(run)
    }

    private fun cancelWaitListPayment(booking: Booking){
        waitListPaymentService.cancelWaitlistSetupIntent(booking.setupIntentId.orEmpty())
    }

    private fun completeFlow(runSession: RunSession){
        val job = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "RAW_STRING",
            payload = runSession.id.orEmpty(),
            traceId = UUID.randomUUID().toString(),
            createdAtMs = Instant.now()
        )

        appScope.launch {
            queueService.sendJob(QueueNames.WAIT_LIST_JOB, job)
        }

    }

}