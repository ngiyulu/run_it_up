package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

data class BookingChangeEvent(
    @Id var id: String?,
    val bookingId: String,
    val userId: String,                // who the booking belongs to
    val actorId: String,               // who made the change (user/admin/system)
    val changeType: ChangeType,
    val oldTotalCents: Long,
    val newTotalCents: Long,
    val deltaCents: Long,              // new - old
    val oldGuests: Int?,
    val newGuests: Int?,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val version: Int                   // monotonically increasing per booking
)

enum class ChangeType {
    BOOKING_CREATED,
    WAITLIST_PROMOTED,
    ADD_GUEST, REMOVE_GUEST,
    PRICE_INCREASE, PRICE_DECREASE,
    PRICE_UPDATE, MANUAL_ADJUSTMENT
}