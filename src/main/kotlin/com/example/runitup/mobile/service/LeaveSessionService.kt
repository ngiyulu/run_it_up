package com.example.runitup.mobile.service

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.web.dto.Role
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.DeleteParticipantFromConversationModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service

@Service
// user decides not to participate anymore
class LeaveSessionService {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

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

     fun execute(request: CancelSessionModel, user:User, admin:AdminUser? = null): RunSession {
         val locale = LocaleContextHolder.getLocale().toString()
         val runDb = runSessionRepository.findById(request.sessionId)
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
         if(!run.isFree()){
             if(run.status != RunStatus.PENDING){
                 cancelPayment(booking)
                 cancelWaitListPayment(booking, run)
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
         return runSessionRepository.save(run)
    }

    private fun cancelWaitListPayment(booking: Booking, run: RunSession){
        booking.intentState?.let {
            waitListPaymentService.cancelSetupIntent(it.intentId)
        }
    }

    private fun cancelPayment(booking: Booking){
        booking.stripePayment.forEach {
            paymentService.cancelHold(it.stripePaymentId)
        }
    }
}