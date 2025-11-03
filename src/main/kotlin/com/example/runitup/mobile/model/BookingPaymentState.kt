package com.example.runitup.mobile.model

import java.time.Instant

/*
Maintains the aggregated payment state for a booking — combining data from all paymentAuthorization documents.
Think of it as a “summary table” that your app can query quickly to see the overall payment status of a booking.

| Field                      | Type                                                          | Description                          |
| -------------------------- | ------------------------------------------------------------- | ------------------------------------ |
| `bookingId`                | String                                                        | Booking this state summarizes        |
| `userId`                   | String                                                        | The user who made the booking        |
| `customerId`               | String                                                        | Stripe Customer ID                   |
| `currency`                 | String                                                        | Currency for all related holds       |
| `totalAuthorizedCents`     | Long                                                          | Sum of all active (authorized) holds |
| `totalCapturedCents`       | Long                                                          | Sum of all captured payments         |
| `totalRefundedCents`       | Long                                                          | Sum of refunds (if any)              |
| `refundableRemainingCents` | Long                                                          | `totalCaptured - totalRefunded`      |
| `status`                   | Enum (`PENDING`, `AUTHORIZED`, `REQUIRES_ACTION`, `CAPTURED`) | Aggregated state                     |
| `latestUpdatedAt`          | Instant                                                       | Last time this state was recomputed  |

 */
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