package com.example.runitup.mobile.rest.v1.controllers.booking

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.getAllBookingStatuses
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetBookingDetailsController: BaseController<String, BookingDetails>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository
    override fun execute(request: String): BookingDetails {
        val booking = bookingRepository.findByRunSessionIdAndStatusIn(request, getAllBookingStatuses().toMutableList() )
        val paidBooking = booking.filter {
            it.paymentStatus == PaymentStatus.PAID ||
                    it.paymentStatus == PaymentStatus.MANUAL_PAID }
        val total = paidBooking.sumOf { it.partySize * it.sessionAmount }
        return BookingDetails(total, booking)
    }

}

data class BookingDetails(val total:Double, val booking:List<Booking>)