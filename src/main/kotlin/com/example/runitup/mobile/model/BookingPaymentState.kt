package com.example.runitup.mobile.model

import java.time.Instant

data class BookingPaymentState(
    val id: String? = null,
    val bookingId: String,
    val userId: String,
    val customerId: String,
    val currency: String = "usd",
    var totalAuthorizedCents: Long = 0,
    var totalCapturedCents: Long = 0,
    var totalRefundedCents: Long = 0,
    var refundableRemainingCents: Long = 0,
    var status: String = "PENDING",    // PENDING, AUTHORIZED, REQUIRES_ACTION, CAPTURED, PARTIALLY_REFUNDED, REFUNDED, CANCELED
    var latestUpdatedAt: Instant =Instant.now()
)