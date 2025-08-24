package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.CancelSessionModel
import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Booking
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.repository.service.BookingDbService
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
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

    override fun execute(request: CancelSessionModel): RunSession {
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
        val booking: Booking = bookingDbService.getBooking(user.id.orEmpty(), run.id.orEmpty())
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