package com.example.runitup.mobile.model

import java.time.Instant


/*
Tracks every individual Stripe PaymentIntent (authorization hold) created for a booking — whether it’s the initial hold (PRIMARY) or any incremental adjustment (DELTA).
It’s a low-level ledger of all payment attempts and their outcomes.
| Field                                                 | Type                                                        | Description                               |
| ----------------------------------------------------- | ----------------------------------------------------------- | ----------------------------------------- |
| `id`                                                  | String                                                      | MongoDB ObjectId                          |
| `bookingId`                                           | String                                                      | The booking this authorization belongs to |
| `userId`                                              | String                                                      | The user who owns the booking             |
| `customerId`                                          | String                                                      | Stripe Customer ID (`cus_...`)            |
| `paymentIntentId`                                     | String                                                      | Stripe PaymentIntent ID (`pi_...`)        |
| `role`                                                | Enum (`PRIMARY`, `DELTA`)                                   | Role of the authorization                 |
| `status`                                              | Enum (`AUTHORIZED`, `CAPTURED`, `CANCELED`, `FAILED`, etc.) | Current state of the PaymentIntent        |
| `amountAuthorizedCents`                               | Long                                                        | The amount Stripe authorized (in cents)   |
| `amountCapturedCents`                                 | Long                                                        | The amount successfully captured (if any) |
| `currency`                                            | String                                                      | e.g., `"usd"`                             |
| `failureKind`                                         | Enum (`NONE`, `TRANSIENT`, `HARD`)                          | Whether and how the authorization failed  |
| `errorType` / `errorCode` / `declineCode` / `message` | String                                                      | Stripe error diagnostic info              |
| `nextRetryAt`                                         | Instant?                                                    | If capture failed and retry is scheduled  |
| `changeEventId`                                       | String                                                      | Links to `BookingChangeEvent` for audit   |
| `createdAt` / `updatedAt`                             | Instant                                                     | Timestamps                                |

*/


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
    var amountRefundedCents: Long = 0,
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

/*
Stripe does not let you “increase” the amount of an existing authorization.
The correct approach is to create an additional hold (a new PaymentIntent) for just the difference (the delta).
That’s why your code creates a new AuthRole.DELTA record whenever total increases.
PRIMARY is the main hold, delta is a new hold that has the difference of the price of the Primary hold and the new request that came in

 */
enum class AuthRole { PRIMARY, DELTA }
enum class AuthStatus { AUTHORIZED, REQUIRES_ACTION, CAPTURED, CANCELED, FAILED, REQUIRES_CAPTURE }

enum class LastOperation { NONE, CREATE, CAPTURE, CANCEL }
