package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
// user decides not to participate anymore
class LeaveSessionController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    override fun execute(request: CancelSessionModel): com.example.runitup.mobile.model.RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val runDb = runSessionRepository.findById(request.sessionId)
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("invalid_user"))
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val run = runDb.get()
        if(!run.isDeletable()){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        val booking: com.example.runitup.mobile.model.Booking = bookingDbService.getBooking(user.id.orEmpty(), run.id.orEmpty())
            ?: throw  ApiRequestException(text("invalid_params"))
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