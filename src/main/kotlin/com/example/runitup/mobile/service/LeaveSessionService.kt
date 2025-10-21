package com.example.runitup.mobile.service

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
// user decides not to participate anymore
class LeaveSessionService() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    @Autowired
    lateinit var cacheManager: MyCacheManager

    @Autowired
    lateinit var textService: TextService

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
        val booking: com.example.runitup.mobile.model.Booking = bookingDbService.getBooking(user.id.orEmpty(), run.id.orEmpty())
            ?: throw  ApiRequestException(textService.getText("invalid_params", locale))
        // this means a hold payment was created so we have to cancel it
        if(run.status != RunStatus.PENDING){
            booking.stripePayment.forEach {
                paymentService.cancelHold(it.stripePaymentId)
            }
        }
        run.bookingList.removeAll {
            it.userId == user.id.orEmpty()
        }

        bookingDbService.cancelUserBooking(user.id.orEmpty())
        return runSessionRepository.save(run)
    }
}