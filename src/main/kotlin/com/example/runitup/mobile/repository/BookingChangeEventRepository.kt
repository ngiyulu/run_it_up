package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.BookingChangeEvent
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.BOOKING_CHANGE_COLLECTION)
interface BookingChangeEventRepository : MongoRepository<BookingChangeEvent, String> {
    fun insert(event: BookingChangeEvent): BookingChangeEvent
    fun findTopByBookingIdOrderByCreatedAtDesc(bookingId: String): BookingChangeEvent?
}