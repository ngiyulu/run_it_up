package com.example.runitup.mobile.model

import java.time.Instant


// booking_authorization_hold (one row per PaymentIntent auth)
// domain/payments/PaymentAuthorization.kt
enum class FailureKind { TRANSIENT, HARD, ACTION_REQUIRED, NONE }

data class PaymentAuthorization(
    val id: String? = null,
    val bookingId: String,
    val userId: String,
    val customerId: String,
    val paymentIntentId: String,
    val role: AuthRole,                         // PRIMARY | DELTA
    val amountAuthorizedCents: Long,
    var amountCapturedCents: Long = 0,
    val currency: String = "usd",
    var status: AuthStatus,                     // AUTHORIZED | REQUIRES_ACTION | CAPTURED | CANCELED | FAILED

    // Failure + retry control (stateful)
    var failureKind: FailureKind = FailureKind.NONE,
    var lastErrorCode: String? = null,          // Stripe error code (e.code)
    var lastDeclineCode: String? = null,        // issuer decline (insufficient_funds, do_not_honor, ...)
    var lastErrorMessage: String? = null,
    var lastFailureAt: Instant? = null,
    var retryCount: Int = 0,
    var nextRetryAt: Long? = null,              // epoch millis; query by this for jobs
    var maxRetries: Int = 2,                    // policy

    // User action notification
    var needsUserAction: Boolean = false,
    var notifiedUserActionAt: Instant? = null,

    // Bookkeeping
    val relatedChangeEventId: String? = null,
    var updatedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    var note:String? = null,
    var lastOperation: LastOperation = LastOperation.NONE
)

enum class AuthRole { PRIMARY, DELTA }
enum class AuthStatus { AUTHORIZED, REQUIRES_ACTION, CAPTURED, CANCELED, FAILED, REQUIRES_CAPTURE }

enum class LastOperation { NONE, CREATE, CAPTURE, CANCEL }
