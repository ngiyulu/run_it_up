package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDate

data class Refund(
    val id: String? = null,                 // DB id
    val refundId: String,                   // Stripe refund id: re_*
    val paymentIntentId: String,            // pi_*
    val chargeId: String,                   // ch_*
    val bookingId: String,                  // your domain id (or sessionId)
    val userId: String,
    val amountCents: Long,
    val currency: String,
    val status: String,                     // pending, succeeded, failed, canceled
    val reason: String? = null,             // REQUESTED_BY_CUSTOMER, etc.
    val balanceTransactionId: String? = null, // for accounting
    val idempotencyKey: String? = null,     // your key used when creating refund
    val createdAt: Long,                    // epoch millis
    val updatedAt: Long,                    // epoch millis
    val source: String,                     // "WEBHOOK" | "API"
    val failureReason: String? = null,      // if failed
    val metadata: Map<String,String> = emptyMap()
)