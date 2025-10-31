// payments/SmartSetupService.kt
package com.example.runitup.mobile.service

import com.stripe.exception.CardException
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.net.RequestOptions
import com.stripe.param.SetupIntentCreateParams
import org.springframework.stereotype.Service

@Service
class WaitListPaymentService {

    /**
     * Ensure an already-saved card is approved for future OFF_SESSION use.
     * It confirms a SetupIntent server-side with the saved payment method.
     *
     * Behavior:
     * - If no extra auth required: returns ok=true, needsUserAction=false (no client step needed).
     * - If 3DS is required: returns ok=false, needsUserAction=true + clientSecret (app must complete).
     *
     * @param customerId Stripe Customer id (cus_...)
     * @param paymentMethodId Optional saved PM id (pm_...). If null, uses customer's default PM.
     * @param idempotencyKey Optional idempotency key for safety in retries.
     * @param metadata Optional metadata to stamp on the SetupIntent (e.g., userId, sessionId).
     */

    private fun createSetupIntentWithIdempotency(
        params: SetupIntentCreateParams,
        idempotencyKey: String?
    ): SetupIntent {
        return if (idempotencyKey.isNullOrBlank()) {
            SetupIntent.create(params)
        } else {
            val opts = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build()
            SetupIntent.create(params, opts)
        }
    }


    fun ensureOffSessionReadyServerSide(
        customerId: String,
        paymentMethodId: String,
        idempotencyKey: String?
    ): SmartSetupResult {
        val params = SetupIntentCreateParams.builder()
            .setCustomer(customerId)
            .setPaymentMethod(paymentMethodId)
            .setConfirm(true)
            .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
            .build()

        return try {
            val si = createSetupIntentWithIdempotency(params, idempotencyKey)
            if (si.status == "requires_action") {
                SmartSetupResult(
                    ok = false,
                    setupIntentId = si.id,
                    status = si.status,
                    needsUserAction = true,
                    clientSecret = si.clientSecret
                )
            } else {
                SmartSetupResult(ok = true, setupIntentId = si.id, status = si.status)
            }
        } catch (e: CardException) {
            val stripeErr = e.stripeError
            val si = stripeErr?.setupIntent  // ✅ correct way to access the SI from the exception
            SmartSetupResult(
                ok = false,
                setupIntentId = si?.id,
                status = si?.status ?: "requires_action",
                needsUserAction = true,
                clientSecret = si?.clientSecret,
                errorCode = e.code,
                errorMessage = e.message
            )
        }
    }

    // when user
    fun getPaymentIntentClientSecret(intentId: String): String {
        val pi = PaymentIntent.retrieve(intentId)
        val secret = pi.clientSecret
        require(!secret.isNullOrBlank()) { "PaymentIntent has no client_secret." }
        return secret
    }
}

data class SmartSetupResult(
    val ok: Boolean,
    val setupIntentId: String? = null,
    val status: String? = null,            // e.g., "succeeded", "requires_action"
    val needsUserAction: Boolean = false,  // true only if 3DS is needed
    val clientSecret: String? = null,      // present only when needsUserAction == true
    val errorCode: String? = null,
    val errorMessage: String? = null
)