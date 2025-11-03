package com.example.runitup.mobile.rest.v1.dto.payment

import com.stripe.model.PaymentIntent

data class CreateChargeResult(
    val ok: Boolean,

    // Success / PI context
    val paymentIntentId: String? = null,
    val clientSecret: String? = null,
    val status: String? = null,                  // e.g. "requires_action", "requires_capture", "succeeded", "requires_payment_method"
    val isHold: Boolean = false,
    val amount: Long? = null,// true => manual capture (authorization/hold)
    val amountCents: Long? = null,
    val currency: String? = null,
    val customerId: String? = null,
    val paymentMethodId: String? = null,
    val idempotencyKey: String? = null,

    // Next action (3DS/SCA)
    val requiresAction: Boolean = false,
    val nextActionType: String? = null,          // e.g. "use_stripe_sdk", "redirect_to_url"
    val nextActionRedirectUrl: String? = null,   // if redirect_to_url

    // Last payment error from PI (non-exceptional failure)
    val lastPaymentErrorCode: String? = null,    // e.g. "authentication_required"
    val lastPaymentErrorDeclineCode: String? = null, // e.g. "insufficient_funds"
    val lastPaymentErrorMessage: String? = null,

    // Exception details (when an exception occurred)
    val errorType: String? = null,               // "card_error", "api_error", etc.
    val errorCode: String? = null,               // SDK-level code like "rate_limit" or CardException.code
    val declineCode: String? = null,             // issuer decline code (if any)
    val message: String? = null,                 // human-readable error

    // Hint to drive retry UX (idempotent retry or ask for another PM)
    val canRetry: Boolean = false,
    val paymentIntent:PaymentIntent? = null
)
