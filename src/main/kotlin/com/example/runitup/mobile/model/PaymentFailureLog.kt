package com.example.runitup.mobile.model

import java.time.Instant

// model/PaymentFailureLog.kt
data class PaymentFailureLog(
    val id: String? = null,
    val bookingId: String,
    val paymentIntentId: String?,
    val userId: String,
    val eventType: String,              // "CREATE_PI" | "CAPTURE_PI" | "CANCEL_PI"
    val failureKind: FailureKind,
    val errorCode: String?,
    val declineCode: String?,
    val message: String?,
    val attempt: Int,                   // 1..N
    val createdAt: Instant = Instant.now(),
    val metadata: Map<String,String> = emptyMap()
)


