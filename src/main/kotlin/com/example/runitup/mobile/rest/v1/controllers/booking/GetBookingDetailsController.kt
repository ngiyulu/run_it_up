package com.example.runitup.mobile.rest.v1.controllers.booking

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.getAllBookingStatuses
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.BookingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetBookingDetailsController: BaseController<String, BookingDetails>() {

    @Autowired
    lateinit var bookingService: BookingService
    override fun execute(request: String): BookingDetails {
       return bookingService.getBookingDetails(request)
    }
}

data class BookingDetails(val total:Double, val booking:List<Booking>)