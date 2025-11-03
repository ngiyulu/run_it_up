package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.BookingChangeEvent
import com.example.runitup.mobile.model.ChangeType
import com.example.runitup.mobile.repository.BookingChangeEventRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ChangeBookingEventService {
    @Autowired
    lateinit var changeBookingChangeEventRepository: BookingChangeEventRepository

    fun createChangeEvent(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        totalCents: Long,
        savedPaymentMethodId: String,
        actorId: String
    ): BookingChangeEvent{
        return changeBookingChangeEventRepository.insert(
            BookingChangeEvent(
                id = ObjectId().toString(),
                bookingId = bookingId,
                userId = userId,
                actorId = actorId,
                changeType = ChangeType.BOOKING_CREATED,
                oldTotalCents = 0,
                newTotalCents = totalCents,
                deltaCents = totalCents,
                oldGuests = null,
                newGuests = null,
                version = nextVersionFor(bookingId)
            )
        )
    }
    fun nextVersionFor(bookingId: String): Int {
        val last = changeBookingChangeEventRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
        return (last?.version ?: 0) + 1
    }
}