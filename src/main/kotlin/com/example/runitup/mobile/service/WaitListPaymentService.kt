// payments/SmartSetupService.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.SetupStatus
import com.example.runitup.mobile.model.WaitlistSetupState
import com.example.runitup.mobile.repository.WaitlistSetupStateRepository
import com.stripe.exception.CardException
import com.stripe.exception.StripeException
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.net.RequestOptions
import com.stripe.param.SetupIntentCancelParams
import com.stripe.param.SetupIntentCreateParams
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WaitListPaymentService {

    @Autowired
    lateinit var waitlistSetupRepo: WaitlistSetupStateRepository
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


    /**
     * Prepare a saved card for OFF_SESSION use for waitlist auto-charge.
     * - Creates/Confirms a SetupIntent server-side with the provided PaymentMethod.
     * - Persists/updates WaitlistSetupState for audit & later decisions.
     */
    fun ensureWaitlistCardReady(
        bookingId: String,
        sessionId: String,
        userId: String,
        customerId: String,
        paymentMethodId: String,
        idempotencyKey: String? = null
    ): WaitlistSetupState {
        val res = ensureOffSessionReadyServerSide(
            customerId = customerId,
            paymentMethodId = paymentMethodId,
            idempotencyKey = idempotencyKey
        )

        val now = System.currentTimeMillis()
        val existing = waitlistSetupRepo.findByBookingIdAndPaymentMethodId(bookingId, paymentMethodId)

        val status = when {
            res.ok && (res.status == "succeeded") -> SetupStatus.SUCCEEDED
            !res.ok && res.needsUserAction        -> SetupStatus.REQUIRES_ACTION
            !res.ok && res.status == "canceled"   -> SetupStatus.CANCELED
            !res.ok                               -> SetupStatus.ERROR
            else                                  -> SetupStatus.UNKNOWN
        }

        val row = (existing ?: WaitlistSetupState(
            bookingId = bookingId,
            sessionId = sessionId,
            userId = userId,
            customerId = customerId,
            paymentMethodId = paymentMethodId,
            setupIntentId = res.setupIntentId
        )).copy(
            setupIntentId = res.setupIntentId ?: existing?.setupIntentId,
            status = status,
            needsUserAction = res.needsUserAction,
            clientSecret = if (res.needsUserAction) res.clientSecret else null,
            errorCode = res.errorCode,
            errorMessage = res.errorMessage,
            updatedAt = now
        )

        val saved = if (existing == null) waitlistSetupRepo.insert(row) else waitlistSetupRepo.save(row)

        // Optional: push notification if SCA needed (dedupe using lastNotifiedAt)
        if (saved.needsUserAction && saved.lastNotifiedAt == null) {
            // pushService.notifyPaymentActionRequired(userId, saved.setupIntentId, saved.clientSecret)
            saved.lastNotifiedAt = System.currentTimeMillis()
            waitlistSetupRepo.save(saved)
        }

        return saved
    }

    /**
     * Re-fetch a SetupIntent and refresh local state (useful if client completed SCA).
     */
    fun refreshWaitlistSetupState(setupIntentId: String): WaitlistSetupState? {
        val si = SetupIntent.retrieve(setupIntentId)
        val row = waitlistSetupRepo.findBySetupIntentId(setupIntentId) ?: return null

        val status = when (si.status) {
            "succeeded" -> SetupStatus.SUCCEEDED
            "requires_action" -> SetupStatus.REQUIRES_ACTION
            "canceled" -> SetupStatus.CANCELED
            else -> SetupStatus.UNKNOWN
        }

        row.status = status
        row.needsUserAction = (status == SetupStatus.REQUIRES_ACTION)
        row.clientSecret = if (row.needsUserAction) si.clientSecret else null
        row.errorCode = null
        row.errorMessage = null
        row.updatedAt = System.currentTimeMillis()

        return waitlistSetupRepo.save(row)
    }

    /**
     * Cancel a SetupIntent and update local state.
     * Note: SetupIntentCancelParams has no specific reason enum; cancel is straightforward.
     */
    fun cancelWaitlistSetupIntent(setupIntentId: String): WaitlistSetupState? {
        val si = SetupIntent.retrieve(setupIntentId)
        val canceled = si.cancel(SetupIntentCancelParams.builder().build())

        val row = waitlistSetupRepo.findBySetupIntentId(setupIntentId) ?: return null
        row.status = SetupStatus.CANCELED
        row.needsUserAction = false
        row.clientSecret = null
        row.updatedAt = System.currentTimeMillis()
        return waitlistSetupRepo.save(row)
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

    /**
     * Cancels a SetupIntent by its ID.
     *
     * - Works only if the SetupIntent is still in a cancellable state (e.g. requires_action, requires_payment_method, or requires_confirmation)
     * - If already succeeded or canceled, Stripe returns the current state (no double error)
     * - This does NOT refund anything (SetupIntents never charge funds)
     */
    fun cancelSetupIntent(intentId: String): CancelIntentResult {
        return try {
            val si = SetupIntent.retrieve(intentId)
            val canceled = si.cancel()
            CancelIntentResult(
                ok = true,
                intentId = canceled.id,
                status = canceled.status,
                message = "SetupIntent successfully canceled."
            )
        } catch (e: StripeException) {
            CancelIntentResult(
                ok = false,
                intentId = intentId,
                status = "error",
                message = e.message
            )
        } catch (e: Exception) {
            CancelIntentResult(
                ok = false,
                intentId = intentId,
                status = "error",
                message = "Unexpected error: ${e.message}"
            )
        }
    }

}


data class CancelIntentResult(
    val ok: Boolean,
    val intentId: String? = null,
    val status: String? = null,
    val message: String? = null
)


data class SmartSetupResult(
    val ok: Boolean,
    val setupIntentId: String? = null,
    val status: String? = null,            // e.g., "succeeded", "requires_action"
    val needsUserAction: Boolean = false,  // true only if 3DS is needed
    val clientSecret: String? = null,      // present only when needsUserAction == true
    val errorCode: String? = null,
    val errorMessage: String? = null
)