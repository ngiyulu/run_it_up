package com.example.runitup.mobile.utility



import com.example.runitup.mobile.service.PIStatus
import com.example.runitup.mobile.service.PaymentIntentRecord
import com.stripe.model.PaymentIntent

object PaymentIntentMapper {
    fun toStatus(pi: PaymentIntent): PIStatus = when (pi.status) {
        "requires_action"  -> PIStatus.REQUIRES_ACTION
        "requires_capture" -> PIStatus.AUTHORIZED
        "succeeded"        -> PIStatus.CAPTURED
        "canceled"         -> PIStatus.CANCELED
        "requires_payment_method" -> PIStatus.FAILED
        else               -> PIStatus.PENDING
    }

    fun fromStripe(
        pi: PaymentIntent,
        bookingId: String,
        userId: String,
        customerId: String,
        authExpiresAt: Long? = null
    ): PaymentIntentRecord = PaymentIntentRecord(
        bookingId = bookingId,
        userId = userId,
        customerId = customerId,
        paymentIntentId = pi.id,
        currency = pi.currency ?: "usd",
        amountAuthorizedCents = pi.amount ?: 0L,
        amountCapturableCents = pi.amountCapturable ?: 0L,
        amountCapturedCents = pi.amountReceived ?: 0L,
        latestChargeId = pi.latestCharge,
        captureMethod = pi.captureMethod ?: "manual",
        status = toStatus(pi),
        authExpiresAt = authExpiresAt
    )

    fun mergeSnapshot(old: PaymentIntentRecord, pi: PaymentIntent): PaymentIntentRecord = old.copy(
        amountCapturableCents = pi.amountCapturable ?: old.amountCapturableCents,
        amountCapturedCents = pi.amountReceived ?: old.amountCapturedCents,
        latestChargeId = pi.latestCharge ?: old.latestChargeId,
        status = toStatus(pi),
        updatedAt = System.currentTimeMillis()
    )
}
