package com.example.runitup.mobile.rest.v1.dto

data class DeltaHoldIncreaseResult(
    val ok: Boolean,
    val bookingId: String,
    val userId: String,
    val deltaCents: Long,
    val paymentIntentId: String? = null,
    val requiresAction: Boolean = false,
    val message: String? = null,
    // Optional error details (when ok=false)
    val errorType: String? = null,
    val errorCode: String? = null,
    val declineCode: String? = null
)

data class DecreaseBeforeCaptureResult(
    val ok: Boolean,
    val bookingId: String,
    val userId: String,
    val oldTotalCents: Long,
    val newTotalCents: Long,
    val largeDrop: Boolean,
    val canceledPrimary: Boolean? = null,
    val canceledPrimaryReason: String? = null,
    val replacedPrimaryPaymentIntentId: String? = null,
    val requiresAction: Boolean = false,
    val message: String? = null,
    // Optional error details (when ok=false)
    val errorType: String? = null,
    val errorCode: String? = null,
    val declineCode: String? = null
)