package com.example.runitup.mobile.service

import com.stripe.exception.StripeException
import com.stripe.model.Charge
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import com.stripe.net.RequestOptions
import com.stripe.param.ChargeListParams
import com.stripe.param.RefundCreateParams
import com.stripe.param.RefundListParams
import org.springframework.stereotype.Service

data class RefundResult(
    val ok: Boolean,
    val refundId: String? = null,
    val status: String? = null,       // pending, succeeded, failed, canceled
    val message: String? = null
)

@Service
class RefundService {

    /**
     * Create a FULL refund for a captured PaymentIntent.
     */
    fun refundFullPaymentIntent(
        paymentIntentId: String,
        reason: RefundCreateParams.Reason? = null,
        idempotencyKey: String = "refund-full-$paymentIntentId"
    ): RefundResult {
        val chargeId = resolveChargeIdFromPaymentIntent(paymentIntentId)
            ?: return RefundResult(false, message = "No charge found for PaymentIntent $paymentIntentId")

        val params = RefundCreateParams.builder()
            .setCharge(chargeId)
            .apply { if (reason != null) setReason(reason) }
            .build()

        return createRefund(params, idempotencyKey)
    }

    /**
     * Create a PARTIAL refund (amount in cents) for a captured PaymentIntent.
     */
    fun refundPartialPaymentIntent(
        paymentIntentId: String,
        amountCents: Long,
        reason: RefundCreateParams.Reason? = null,
        idempotencyKey: String = "refund-partial-$paymentIntentId-$amountCents"
    ): RefundResult {
        require(amountCents > 0) { "amountCents must be > 0" }
        val chargeId = resolveChargeIdFromPaymentIntent(paymentIntentId)
            ?: return RefundResult(false, message = "No charge found for PaymentIntent $paymentIntentId")

        val params = RefundCreateParams.builder()
            .setCharge(chargeId)
            .setAmount(amountCents)
            .apply { if (reason != null) setReason(reason) }
            .build()

        return createRefund(params, idempotencyKey)
    }

    /**
     * Refund directly by charge id (full or partial).
     */
    fun refundByCharge(
        chargeId: String,
        amountCents: Long? = null,
        reason: RefundCreateParams.Reason? = null,
        idempotencyKey: String = "refund-by-charge-$chargeId-${amountCents ?: "full"}"
    ): RefundResult {
        val builder = RefundCreateParams.builder().setCharge(chargeId)
        if (amountCents != null) builder.setAmount(amountCents)
        if (reason != null) builder.setReason(reason)
        return createRefund(builder.build(), idempotencyKey)
    }

    /**
     * Retrieve a refund.
     */
    fun getRefund(refundId: String): Refund? = try {
        Refund.retrieve(refundId)
    } catch (_: Exception) { null }

    /**
     * List refunds for a PaymentIntent (via charge).
     */
    fun listRefundsForPaymentIntent(paymentIntentId: String, limit: Long = 10): List<Refund> {
        val chargeId = resolveChargeIdFromPaymentIntent(paymentIntentId) ?: return emptyList()
        val params = RefundListParams.builder()
            .setCharge(chargeId)
            .setLimit(limit)
            .build()
        return Refund.list(params).data
    }


    // ---------- Internals ----------

    private fun createRefund(params: RefundCreateParams, idempotencyKey: String): RefundResult {
        return try {
            val opts = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build()
            val refund = Refund.create(params, opts)
            RefundResult(true, refund.id, refund.status, "Refund created")
        } catch (e: StripeException) {
            RefundResult(false, null, "error", e.message)
        } catch (e: Exception) {
            RefundResult(false, null, "error", "Unexpected error: ${e.message}")
        }
    }

    /**
     * Finds the charge to refund from a PaymentIntent.
     * We expand 'latest_charge' and fall back to charges[].first() for robustness.
     */
    private fun resolveChargeIdFromPaymentIntent(paymentIntentId: String): String? {
        return try {
            val pi = PaymentIntent.retrieve(paymentIntentId)
            pi.latestCharge // returns "ch_..." or null
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveChargeIdViaListing(paymentIntentId: String): String? {
        return try {
            val params = ChargeListParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setLimit(1) // latest first
                .build()
            val charges = Charge.list(params).data
            charges.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    /*
    resolveChargeIdFromPaymentIntent() is just a small helper you add to your backend to find the Stripe charge_id (e.g. "ch_123") linked to a given PaymentIntent.
You need that charge ID whenever you want to do things such as refunds, receipts, or manual reconciliation.
So resolveChargeIdFromPaymentIntent() is simply:

“Given a PaymentIntent ID, find the associated Charge ID.”
     */
    fun resolveChargeId(paymentIntentId: String): String? {
        val latest = resolveChargeIdFromPaymentIntent(paymentIntentId)
        if (!latest.isNullOrBlank()) return latest
        return resolveChargeIdViaListing(paymentIntentId)
    }
}
