// service/BookingPricingAdjuster.kt
package com.example.runitup.mobile.service


import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingChangeEventRepository
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.rest.v1.dto.DecreaseBeforeCaptureResult
import com.example.runitup.mobile.rest.v1.dto.DeltaHoldIncreaseResult
import com.stripe.param.PaymentIntentCancelParams
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

data class PrimaryHoldResult(
    val ok: Boolean,
    val message: String? = null,
    val paymentIntentId: String? = null
)


@Service
class BookingPricingAdjuster(
    private val paymentService: PaymentService,
    private val changeRepo: BookingChangeEventRepository,
    private val authRepo: PaymentAuthorizationRepository,
    private val bookingStateRepo: BookingPaymentStateRepository,
    private val  paymentAuthorizationService: PaymentAuthorizationService,
    private val  changeBookingEventService: ChangeBookingEventService
) {



    fun createPrimaryHoldWithChange(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        totalCents: Long,
        savedPaymentMethodId: String,
        actorId: String,
    ): PrimaryHoldResult {
        return try {
            // 1. Create change event for audit trail
            val change = changeRepo.insert(
                BookingChangeEvent(
                    id = ObjectId().toString(),
                    bookingId = bookingId,
                    userId = userId,
                    actorId = actorId,
                    changeType = ChangeType.BOOKING_CREATED,
                    oldTotalCents = 0,
                    newTotalCents = totalCents,
                    deltaCents = totalCents,
                    oldGuests = null,
                    newGuests = null,
                    version = changeBookingEventService.nextVersionFor(bookingId)
                )
            )

            // 2. Create Stripe PaymentIntent (manual capture)
            val create = paymentService.createCharge(
                isHold = true,
                amountCents = totalCents,
                currency = currency,
                paymentMethodId = savedPaymentMethodId,
                customerId = customerId,
                idempotencyKey = "primary-$bookingId-$userId"
            )

            if (!create.ok || create.paymentIntentId == null) {
                paymentAuthorizationService.recordCreateFailureNoRetry(
                    bookingId = bookingId,
                    userId = userId,
                    customerId = customerId,
                    currency = currency,
                    role = AuthRole.PRIMARY,
                    changeEventId = change.id,
                    result = create
                )
                // Update aggregate state (FAILED or REQUIRES_ACTION)
                updateAggregateState(bookingId, customerId, currency)
                return PrimaryHoldResult(ok = false, message = create.message ?: "Failed to create primary hold")
            }


            // 3. Map it to PaymentAuthorization in DB
            paymentAuthorizationService.upsertPaymentAuthorizationFromCreate(
                bookingId = bookingId,
                userId = userId,
                customerId = customerId,
                currency = currency,
                role = AuthRole.PRIMARY,
                changeEventId = change.id,
                create = create
            )

            // 4. Update aggregate payment state
            updateAggregateState(bookingId, customerId, currency)

            // 5. Return result
            PrimaryHoldResult(
                ok = create.ok,
                message = create.message ?: "Primary hold created",
                paymentIntentId = create.paymentIntentId
            )
        } catch (e: Exception) {
            PrimaryHoldResult(
                ok = false,
                message = "Error creating primary hold: ${e.message}"
            )
        }
    }

    fun createDeltaHoldForIncrease(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        oldTotalCents: Long,
        newTotalCents: Long,
        oldGuests: Int?,
        newGuests: Int?,
        savedPaymentMethodId: String,
        actorId: String
    ): DeltaHoldIncreaseResult {
        val delta = newTotalCents - oldTotalCents
        if (delta <= 0) {
            return DeltaHoldIncreaseResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                deltaCents = delta,
                message = "Delta must be positive for an increase."
            )
        }

        return try {
            // 1) Change event (audit)
            val change = changeRepo.insert(
                BookingChangeEvent(
                    id = org.bson.types.ObjectId().toHexString(),
                    bookingId = bookingId,
                    userId = userId,
                    actorId = actorId,
                    changeType = if ((newGuests ?: 0) > (oldGuests ?: 0))
                        ChangeType.ADD_GUEST else ChangeType.PRICE_INCREASE,
                    oldTotalCents = oldTotalCents,
                    newTotalCents = newTotalCents,
                    deltaCents = delta,
                    oldGuests = oldGuests,
                    newGuests = newGuests,
                    version = changeBookingEventService.nextVersionFor(bookingId)
                )
            )

            // 2) Create DELTA hold (manual capture)
            val create = paymentService.createCharge(
                isHold = true,
                amountCents = delta,
                currency = currency,
                paymentMethodId = savedPaymentMethodId,
                customerId = customerId,
                idempotencyKey = "delta-$bookingId-$userId-$delta"
            )

            if (!create.ok || create.paymentIntentId == null) {
                // No retries for hold creation — surface error directly
                return DeltaHoldIncreaseResult(
                    ok = false,
                    bookingId = bookingId,
                    userId = userId,
                    deltaCents = delta,
                    message = create.message ?: "Stripe hold creation failed",
                    errorType = create.errorType ?: if (create.requiresAction) "action_required" else null,
                    errorCode = create.errorCode ?: create.lastPaymentErrorCode,
                    declineCode = create.declineCode ?: create.lastPaymentErrorDeclineCode,
                    requiresAction = create.requiresAction
                )
            }

            // 3) Persist PaymentAuthorization (DELTA)
            paymentAuthorizationService.upsertPaymentAuthorizationFromCreate(
                bookingId = bookingId,
                userId = userId,
                customerId = customerId,
                currency = currency,
                role = AuthRole.DELTA,
                changeEventId = change.id,
                create = create
            )

            // 4) Update aggregate state
            updateAggregateState(bookingId, customerId, currency)

            DeltaHoldIncreaseResult(
                ok = true,
                bookingId = bookingId,
                userId = userId,
                deltaCents = delta,
                paymentIntentId = create.paymentIntentId,
                requiresAction = create.requiresAction,
                message = create.message ?: "Delta hold authorized"
            )
        } catch (e: Exception) {
            DeltaHoldIncreaseResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                deltaCents = delta,
                message = "Unexpected error creating delta hold: ${e.message}"
            )
        }
    }


    fun handleDecreaseBeforeCapture(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        oldTotalCents: Long,
        newTotalCents: Long,
        primaryPaymentIntentId: String?,
        savedPaymentMethodId: String?,
        actorId: String,
        thresholdForReauthCents: Long = 500L
    ): DecreaseBeforeCaptureResult {
        val delta = newTotalCents - oldTotalCents
        if (delta >= 0) {
            return DecreaseBeforeCaptureResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = false,
                message = "Delta must be negative for a decrease."
            )
        }

        // 1) Change event (audit)
        val bookingEvent = try {
            changeRepo.insert(
                BookingChangeEvent(
                    id = org.bson.types.ObjectId().toHexString(),
                    bookingId = bookingId,
                    userId = userId,
                    actorId = actorId,
                    changeType = ChangeType.PRICE_DECREASE,
                    oldTotalCents = oldTotalCents,
                    newTotalCents = newTotalCents,
                    deltaCents = delta,
                    oldGuests = null,
                    newGuests = null,
                    version = nextVersionFor(bookingId)
                )
            )
        } catch (e: Exception) {
            return DecreaseBeforeCaptureResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = false,
                message = "Failed to record change event: ${e.message}"
            )
        }

        val largeDrop = -delta >= thresholdForReauthCents

        // If not a large drop, keep existing auth; update state and return success
        if (!largeDrop) {
            updateAggregateState(bookingId, customerId, currency)
            return DecreaseBeforeCaptureResult(
                ok = true,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = false,
                message = "Decrease recorded; existing authorization will be captured for lower total."
            )
        }

        // Large drop path requires we replace the primary auth
        if (primaryPaymentIntentId == null || savedPaymentMethodId == null) {
            updateAggregateState(bookingId, customerId, currency)
            return DecreaseBeforeCaptureResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = true,
                message = "Cannot re-authorize without existing primary PI and a saved payment method."
            )
        }

        return try {
            // 2) Cancel old primary hold on Stripe (structured result)
            val cancel = paymentService.cancelHold(primaryPaymentIntentId)
            if (!cancel.ok) {
                // Reflect cancel failure in DB state as FAILED (your PaymentAuthorizationService already has helpers)
                paymentAuthorizationService.markCanceled(
                    paymentIntentId = primaryPaymentIntentId,
                    errorType = cancel.errorType,
                    errorCode = cancel.errorCode,
                    declineCode = cancel.declineCode,
                    message = cancel.message
                )
                updateAggregateState(bookingId, customerId, currency)
                return DecreaseBeforeCaptureResult(
                    ok = false,
                    bookingId = bookingId,
                    userId = userId,
                    oldTotalCents = oldTotalCents,
                    newTotalCents = newTotalCents,
                    largeDrop = true,
                    canceledPrimary = false,
                    canceledPrimaryReason = cancel.message,
                    message = "Failed to cancel previous authorization.",
                    errorType = cancel.errorType,
                    errorCode = cancel.errorCode,
                    declineCode = cancel.declineCode
                )
            }

            // Mark canceled locally
            paymentAuthorizationService.markCanceled(primaryPaymentIntentId)

            // 3) Re-authorize a fresh PRIMARY hold at the lower total
            val create = paymentService.createCharge(
                isHold = true,
                amountCents = newTotalCents,
                currency = currency,
                paymentMethodId = savedPaymentMethodId,
                customerId = customerId,
                idempotencyKey = "primary-replace-$bookingId-$userId-$newTotalCents"
            )

            if (!create.ok || create.paymentIntentId == null) {
                updateAggregateState(bookingId, customerId, currency)
                return DecreaseBeforeCaptureResult(
                    ok = false,
                    bookingId = bookingId,
                    userId = userId,
                    oldTotalCents = oldTotalCents,
                    newTotalCents = newTotalCents,
                    largeDrop = true,
                    canceledPrimary = true,
                    canceledPrimaryReason = cancel.cancellationReason ?: "canceled",
                    message = create.message ?: "Re-authorization failed",
                    requiresAction = create.requiresAction,
                    errorType = create.errorType ?: if (create.requiresAction) "action_required" else null,
                    errorCode = create.errorCode ?: create.lastPaymentErrorCode,
                    declineCode = create.declineCode ?: create.lastPaymentErrorDeclineCode
                )
            }

            paymentAuthorizationService.upsertPaymentAuthorizationFromCreate(
                bookingId = bookingId,
                userId = userId,
                customerId = customerId,
                currency = currency,
                role = AuthRole.PRIMARY,
                changeEventId = bookingEvent.id,
                create = create
            )

            updateAggregateState(bookingId, customerId, currency)

            DecreaseBeforeCaptureResult(
                ok = true,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = true,
                canceledPrimary = true,
                canceledPrimaryReason = cancel.cancellationReason ?: "canceled",
                replacedPrimaryPaymentIntentId = create.paymentIntentId,
                requiresAction = create.requiresAction,
                message = create.message ?: "Primary hold replaced at lower amount"
            )
        } catch (e: Exception) {
            updateAggregateState(bookingId, customerId, currency)
            DecreaseBeforeCaptureResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = true,
                message = "Unexpected error during re-authorization: ${e.message}"
            )
        }
    }


    /**
     * Capture all holds for a booking up to finalTotalCents.
     * Policy: capture DELTA first, then PRIMARY. Partial capture allowed.
     */
    fun captureBookingHolds(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        finalTotalCents: Long
    ): CaptureBookingResult {
        val msgs = mutableListOf<String>()
        var remaining = finalTotalCents
        var capturedTotal = 0L

        // Get authorizations in desired order: DELTA first, then PRIMARY
        val auths = authRepo.findByBookingIdOrderByRoleAscCreatedAtAsc(bookingId)
            .filter { it.status == AuthStatus.AUTHORIZED || it.status == AuthStatus.REQUIRES_ACTION || it.status == AuthStatus.REQUIRES_CAPTURE }

        if (auths.isEmpty()) {
            return CaptureBookingResult(ok = false, capturedTotalCents = 0, remainingToCaptureCents = finalTotalCents,
                messages = listOf("No capturable authorizations found for booking $bookingId"))
        }

        for (pa in auths) {
            if (remaining <= 0) break

            // Determine how much we could capture from this PI
            val maxForThisPI = (pa.amountAuthorizedCents - pa.amountCapturedCents).coerceAtLeast(0)
            if (maxForThisPI <= 0) continue

            val amountToCapture = remaining.coerceAtMost(maxForThisPI)
            if (amountToCapture <= 0) continue

            val idemp = "cap-${pa.paymentIntentId}-${UUID.randomUUID()}" // unique per attempt

            val cap = paymentService.captureHold(
                paymentIntentId = pa.paymentIntentId,
                captureAmountCents = amountToCapture,
                idempotencyKey = idemp
            )

            if (cap.ok) {
                // Success – update PA row
                pa.amountCapturedCents = (pa.amountCapturedCents + (cap.amountCapturedCents ?: amountToCapture))
                pa.status = if (pa.amountCapturedCents >= pa.amountAuthorizedCents) AuthStatus.CAPTURED else AuthStatus.CAPTURED // captured (Stripe PI itself becomes 'succeeded')
                pa.failureKind = FailureKind.NONE
                pa.lastErrorCode = null
                pa.lastDeclineCode = null
                pa.lastErrorMessage = null
                pa.nextRetryAt = null
                pa.updatedAt = Instant.now()
                authRepo.save(pa)

                capturedTotal += (cap.amountCapturedCents ?: amountToCapture)
                remaining = (finalTotalCents - capturedTotal).coerceAtLeast(0)
                msgs += "Captured ${cap.amountCapturedCents ?: amountToCapture} from PI ${pa.paymentIntentId}"
            } else {
                // Failure – schedule retry (only for captures)
                paymentAuthorizationService.recordCaptureFailureAndMaybeRetry(
                    paymentIntentId = pa.paymentIntentId,
                    bookingId = bookingId,
                    userId = userId,
                    customerId = customerId,
                    currency = currency,
                    result = cap
                )
                msgs += "Capture failed for PI ${pa.paymentIntentId}: ${cap.message ?: cap.errorCode}"
                // Do not proceed to next PI if your business logic wants to stop on first failure.
                // If you prefer "best effort", continue to next authorization to capture remaining.
            }
        }

        // Update aggregate payment state
        updateAggregateState(bookingId, customerId, currency)

        val ok = remaining == 0L
        return CaptureBookingResult(
            ok = ok,
            capturedTotalCents = capturedTotal,
            remainingToCaptureCents = remaining,
            messages = msgs
        )
    }


    /**
     * Cancel a manual-capture PaymentIntent and update local DB state.
     * Idempotent: if PA already canceled/succeeded, we won't error.
     */
    fun cancelAuthorizationAndUpdate(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        paymentIntentId: String,
        reason: PaymentIntentCancelParams.CancellationReason? = null
    ): CancelAuthorizationResult {

        // 0) Find the PA row (if any). We keep this optional so we can still call Stripe even if DB missing.
        val pa = authRepo.findByPaymentIntentId(paymentIntentId)

        // If already terminal, short-circuit
        if (pa != null && (pa.status == AuthStatus.CANCELED || pa.status == AuthStatus.CAPTURED)) {
            updateAggregateState(bookingId, customerId, currency)
            return CancelAuthorizationResult(
                ok = true,
                message = "PaymentIntent already ${pa.status}",
                paymentIntentId = paymentIntentId,
                stripeStatus = pa.status.name.lowercase()
            )
        }

        // 1) Cancel on Stripe
        val cancel = paymentService.cancelHold(paymentIntentId, reason)

        // 2) Update PA row based on Stripe result
        if (pa != null) {
            if (cancel.ok) {
                pa.status = AuthStatus.CANCELED
                pa.lastErrorCode = null
                pa.lastDeclineCode = null
                pa.lastErrorMessage = null
                pa.failureKind = FailureKind.NONE
                pa.nextRetryAt = null
                pa.updatedAt = Instant.now()
                authRepo.save(pa)
            } else {
                // Non-OK: keep PA present but reflect failure (useful for audit)
                // (Cancel usually shouldn’t be retried; treat as HARD unless it was api_error)
                pa.status = AuthStatus.FAILED
                pa.failureKind = if (cancel.errorType == "api_error" && cancel.canRetry) FailureKind.TRANSIENT else FailureKind.HARD
                pa.lastErrorCode = cancel.errorCode
                pa.lastDeclineCode = cancel.declineCode
                pa.lastErrorMessage = cancel.message
                pa.updatedAt = Instant.now()
                authRepo.save(pa)
            }
        }

        // 3) Update aggregate state for the booking (regardless of PA existence)
        updateAggregateState(bookingId, customerId, currency)

        // 4) Return a small, clean DTO for controllers
        return if (cancel.ok) {
            CancelAuthorizationResult(
                ok = true,
                message = cancel.message,
                paymentIntentId = cancel.paymentIntentId,
                stripeStatus = cancel.status,
                cancellationReason = cancel.cancellationReason
            )
        } else {
            CancelAuthorizationResult(
                ok = false,
                message = cancel.message ?: "Unable to cancel PaymentIntent",
                paymentIntentId = paymentIntentId,
                stripeStatus = cancel.status
            )
        }
    }




    private fun nextVersionFor(bookingId: String): Int {
        val last = changeRepo.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
        return (last?.version ?: 0) + 1
    }

    private fun updateAggregateState(bookingId: String, customerId: String, currency: String) {
        val auths = authRepo.findByBookingId(bookingId)

        val totalAuthorized = auths
            .filter { it.status == AuthStatus.AUTHORIZED || it.status == AuthStatus.REQUIRES_ACTION }
            .sumOf { it.amountAuthorizedCents }

        val totalCaptured = auths
            .filter { it.status == AuthStatus.CAPTURED }
            .sumOf { it.amountCapturedCents }

        val anyRequiresAction = auths.any { it.status == AuthStatus.REQUIRES_ACTION }

        val state = bookingStateRepo.findByBookingId(bookingId)
            ?: BookingPaymentState(
                bookingId = bookingId,
                userId = auths.firstOrNull()?.userId ?: "",
                customerId = customerId,
                currency = currency
            )

        state.totalAuthorizedCents = totalAuthorized
        state.totalCapturedCents = totalCaptured
        // Note: totalRefundedCents should be updated by your RefundService/webhooks
        state.refundableRemainingCents = (state.totalCapturedCents - state.totalRefundedCents).coerceAtLeast(0)
        state.status = when {
            totalCaptured > 0 -> "CAPTURED"
            anyRequiresAction -> "REQUIRES_ACTION"
            totalAuthorized > 0 -> "AUTHORIZED"
            else -> "PENDING"
        }
        state.latestUpdatedAt = Instant.now()
        bookingStateRepo.save(state)
    }

    // this is for capture single payment intent, it will be used later
    fun captureSinglePI(
        paymentIntentId: String,
        captureAmountCents: Long,
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String
    ): Boolean {
        val idemp = "cap-one-$paymentIntentId-${UUID.randomUUID()}"
        val cap = paymentService.captureHold(paymentIntentId, captureAmountCents, idemp)

        return if (cap.ok) {
            val pa = authRepo.findByPaymentIntentId(paymentIntentId) ?: return true
            pa.amountCapturedCents = (pa.amountCapturedCents + (cap.amountCapturedCents ?: captureAmountCents))
            pa.status = AuthStatus.CAPTURED
            pa.failureKind = FailureKind.NONE
            pa.lastErrorCode = null
            pa.lastDeclineCode = null
            pa.lastErrorMessage = null
            pa.nextRetryAt = null
            pa.updatedAt = Instant.now()
            authRepo.save(pa)
            updateAggregateState(bookingId, customerId, currency)
            true
        } else {
            paymentAuthorizationService.recordCaptureFailureAndMaybeRetry(
                paymentIntentId, bookingId, userId, customerId, currency, cap
            )
            updateAggregateState(bookingId, customerId, currency)
            false
        }
    }


}
data class CaptureBookingResult(
    val ok: Boolean,
    val capturedTotalCents: Long,
    val remainingToCaptureCents: Long,
    val messages: List<String> = emptyList()
)

data class CancelAuthorizationResult(
    val ok: Boolean,
    val message: String? = null,
    val paymentIntentId: String? = null,
    val stripeStatus: String? = null,
    val cancellationReason: String? = null
)