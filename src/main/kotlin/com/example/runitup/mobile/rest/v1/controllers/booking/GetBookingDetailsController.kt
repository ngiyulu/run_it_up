package com.example.runitup.mobile.rest.v1.controllers.booking

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.BookingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetBookingDetailsController: BaseController<String, BookingDetails>() {

    @Autowired
    lateinit var bookingService: BookingService
    override fun execute(request: String): BookingDetails {
        val runSession = cacheManager.getRunSession(request)?: throw  ApiRequestException("session_not_found")
        return bookingService.getBookingDetails(runSession)
    }
}

data class BookingDetails(val total:Double, val booking:List<Booking>)