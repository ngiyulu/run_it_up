package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetRunSessionDetailController: BaseController<String, RunSession>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var authRepo: PaymentAuthorizationRepository

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository

    override fun execute(request: String): RunSession {
        val run = runSessionService.getRunSession(request) ?: throw  ApiRequestException("not_found")
        run.bookings.forEach {
            it.bookingPaymentState = bookingPaymentStateRepository.findByBookingId(it.id.orEmpty())
            val active = authRepo.findByBookingId(it.id.orEmpty())
            it.paymentAuthorization = active
        }
        return  run
    }
}