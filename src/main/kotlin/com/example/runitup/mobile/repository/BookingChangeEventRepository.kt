package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.BookingChangeEvent
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.BOOKING_CHANGE_COLLECTION)
interface BookingChangeEventRepository : MongoRepository<BookingChangeEvent, String> {
    // Existing
    fun findTopByBookingIdOrderByCreatedAtDesc(bookingId: String): BookingChangeEvent?

    // 🔎 By user (paged, newest first)
    fun findByUserIdOrderByCreatedAtDesc(userId: String, pageable: org.springframework.data.domain.Pageable)
            : org.springframework.data.domain.Page<BookingChangeEvent>

    // 🔎 By user in time range (optional helper)
    fun findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        userId: String,
        startInclusive: Long,
        endExclusive: Long,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<BookingChangeEvent>

    // 🔎 Full timeline for a booking (version order is ideal for diffs)
    fun findAllByBookingIdOrderByVersionAsc(bookingId: String): List<BookingChangeEvent>

    // Or if you page by time:
    fun findByBookingIdOrderByCreatedAtDesc(bookingId: String, pageable: org.springframework.data.domain.Pageable)
            : org.springframework.data.domain.Page<BookingChangeEvent>


}