package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.BookingPaymentState
import com.example.runitup.mobile.model.PaymentAuthorization
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.BOOKING_STATE_COLLECTION)
interface BookingPaymentStateRepository : MongoRepository<BookingPaymentState, String> {
    fun findByBookingId(bookingId: String): BookingPaymentState?
    fun save(state: BookingPaymentState): BookingPaymentState
}