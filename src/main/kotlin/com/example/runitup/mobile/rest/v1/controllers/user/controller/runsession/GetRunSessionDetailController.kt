package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.repository.service.BookingDbService
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
    lateinit var bookingDbService: BookingDbService


    override fun execute(request: String): RunSession {
        val run = runSessionService.getRunSession(request) ?: throw  ApiRequestException("not_found")
        run.updateStatus()
        run.bookings = run.bookings.map {
            bookingDbService.getBookingDetails(it)
        }.toMutableList()
        return  run
    }
}