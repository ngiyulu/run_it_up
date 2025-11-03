package com.example.runitup.mobile.rest.v1.dto.payment


data class RefundResult(
    val ok: Boolean,
    val bookingId: String? = null,
    val userId: String? = null,
    val paymentIntentId: String? = null,
    val stripeRefundId: String? = null,
    val amountCents: Long = 0,
    val message: String? = null,
    val errorType: String? = null,
    val errorCode: String? = null,
    val declineCode: String? = null
)