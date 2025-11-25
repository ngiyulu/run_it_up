package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.model.getAllBookingStatuses
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.booking.BookingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BookingService {
    @Autowired
    lateinit var bookingRepository: BookingRepository

    fun getBookingDetails(sessionId: String): BookingDetails {
        val booking = bookingRepository.findByRunSessionIdAndStatusIn(sessionId, getAllBookingStatuses().toMutableList() )
        val paidBooking = booking.filter {
            it.paymentStatus == PaymentStatus.PAID ||
                    it.paymentStatus == PaymentStatus.MANUAL_PAID }
        val total = paidBooking.sumOf { it.partySize * it.sessionAmount }
        return BookingDetails(total, booking)
    }
}