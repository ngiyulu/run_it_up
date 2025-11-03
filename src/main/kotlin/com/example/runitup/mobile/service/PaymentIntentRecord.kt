package com.example.runitup.mobile.service


data class PaymentIntentRecord(
    val id: String? = null,                 // DB id
    val bookingId: String,
    val userId: String,
    val customerId: String,
    val paymentIntentId: String,            // pi_*
    val currency: String = "usd",

    // Stripe amounts (cents)
    val amountAuthorizedCents: Long,        // PI.amount (original authorization)
    val amountCapturableCents: Long,        // PI.amount_capturable
    val amountCapturedCents: Long,          // PI.amount_received

    // Stripe refs
    val latestChargeId: String? = null,     // ch_*
    val captureMethod: String = "manual",   // manual | automatic (you use manual)
    val status: PIStatus,                   // normalized

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val authExpiresAt: Long? = null,        // optional: track 7-day window if you want
    val metadata: Map<String, String> = emptyMap()
)

enum class PIStatus {
    REQUIRES_ACTION, AUTHORIZED, CAPTURED, CANCELED, FAILED, PENDING
}
