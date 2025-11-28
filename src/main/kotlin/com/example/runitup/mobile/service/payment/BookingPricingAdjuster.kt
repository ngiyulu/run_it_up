// service/BookingPricingAdjuster.kt
package com.example.runitup.mobile.service.payment

import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingChangeEventRepository
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.rest.v1.dto.DecreaseBeforeCaptureResult
import com.example.runitup.mobile.rest.v1.dto.DeltaHoldIncreaseResult
import com.example.runitup.mobile.service.ChangeBookingEventService
import com.example.runitup.mobile.service.PaymentAuthorizationService
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.myLogger
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

@Service
class BookingPricingAdjuster(
    private val paymentService: PaymentService,
    private val changeRepo: BookingChangeEventRepository,
    private val authRepo: PaymentAuthorizationRepository,
    private val bookingStateRepo: BookingPaymentStateRepository,
    private val paymentAuthorizationService: PaymentAuthorizationService,
    private val changeBookingEventService: ChangeBookingEventService
): BasePaymentService(authRepo, bookingStateRepo) {

    private val logger = myLogger()
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
            // 1) Audit
            val change = changeRepo.insert(
                BookingChangeEvent(
                    id = ObjectId().toHexString(),
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

            // 2) Stripe PI (manual capture)
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
                updateAggregateState(bookingId, customerId, currency)
                return PrimaryHoldResult(ok = false, message = create.message ?: "Failed to create primary hold")
            }

            // 3) Persist PA
            paymentAuthorizationService.upsertPaymentAuthorizationFromCreate(
                bookingId, userId, customerId, currency,
                role = AuthRole.PRIMARY,
                changeEventId = change.id,
                create = create
            )

            // 4) Aggregates
            updateAggregateState(bookingId, customerId, currency)

            PrimaryHoldResult(
                ok = true,
                message = create.message ?: "Primary hold created",
                paymentIntentId = create.paymentIntentId
            )
        } catch (e: Exception) {
            logger.error("createPrimaryHoldWithChange failed ${e.message}")
            PrimaryHoldResult(ok = false, message = "Error creating primary hold: ${e.message}")
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
            // 1) Audit
            val change = changeRepo.insert(
                BookingChangeEvent(
                    id = ObjectId().toHexString(),
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

            // 2) Delta hold
            val create = paymentService.createCharge(
                isHold = true,
                amountCents = delta,
                currency = currency,
                paymentMethodId = savedPaymentMethodId,
                customerId = customerId,
                idempotencyKey = "delta-$bookingId-$userId-$delta"
            )

            if (!create.ok || create.paymentIntentId == null) {
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

            // 3) Persist PA (DELTA)
            paymentAuthorizationService.upsertPaymentAuthorizationFromCreate(
                bookingId, userId, customerId, currency,
                role = AuthRole.DELTA,
                changeEventId = change.id,
                create = create
            )

            // 4) Aggregates
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
            logger.error("Error creating primary hold: ${e.message}")
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

        // 1) Audit
        val bookingEvent = try {
            changeRepo.insert(
                BookingChangeEvent(
                    id = ObjectId().toHexString(),
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
            logger.error("handleDecreaseBeforeCapture failed ${e.message}")
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

        // 2) Small decrease: keep auth, capture lower later
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

        // 3) Large drop requires replacing primary auth
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

        // 4) Cancel ALL active auths (PRIMARY + DELTAs) so we don't double-authorize totals
        val cancels = cancelAllActiveAuthsForBooking(bookingId, userId, customerId, currency)
        val anyHardFail = cancels.any { !it.ok && it.stripeStatus != "canceled" }

        if (anyHardFail) {
            // Aggregates already updated inside cancelAuthorizationAndUpdate
            return DecreaseBeforeCaptureResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                oldTotalCents = oldTotalCents,
                newTotalCents = newTotalCents,
                largeDrop = true,
                canceledPrimary = false,
                canceledPrimaryReason = cancels.firstOrNull { !it.ok }?.message,
                message = "Could not cancel existing authorizations; aborting re-authorization."
            )
        }

        // 5) Re-authorize a fresh PRIMARY hold at the lower total
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
                canceledPrimary = true, // safe: previous PI was canceled
                canceledPrimaryReason = cancels.firstOrNull { !it.ok }?.message,
                message = create.message ?: "Re-authorization failed",
                requiresAction = create.requiresAction,
                errorType = create.errorType ?: if (create.requiresAction) "action_required" else null,
                errorCode = create.errorCode ?: create.lastPaymentErrorCode,
                declineCode = create.declineCode ?: create.lastPaymentErrorDeclineCode
            )
        }

        paymentAuthorizationService.upsertPaymentAuthorizationFromCreate(
            bookingId, userId, customerId, currency,
            role = AuthRole.PRIMARY,
            changeEventId = bookingEvent.id,
            create = create
        )

        updateAggregateState(bookingId, customerId, currency)

        return DecreaseBeforeCaptureResult(
            ok = true,
            bookingId = bookingId,
            userId = userId,
            oldTotalCents = oldTotalCents,
            newTotalCents = newTotalCents,
            largeDrop = true,
            canceledPrimary = true,
            canceledPrimaryReason = cancels.firstOrNull { !it.ok }?.message,
            replacedPrimaryPaymentIntentId = create.paymentIntentId,
            requiresAction = create.requiresAction,
            message = create.message ?: "Primary hold replaced at lower amount"
        )
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

        // Order: DELTA first, then PRIMARY
        val auths = authRepo.findByBookingIdOrderByRoleAscCreatedAtAsc(bookingId)
            .filter { it.status == AuthStatus.AUTHORIZED || it.status == AuthStatus.REQUIRES_ACTION || it.status == AuthStatus.REQUIRES_CAPTURE }

        if (auths.isEmpty()) {
            return CaptureBookingResult(
                ok = false,
                capturedTotalCents = 0,
                remainingToCaptureCents = finalTotalCents,
                messages = listOf("No capturable authorizations found for booking $bookingId")
            )
        }

        for (pa in auths) {
            if (remaining <= 0) break
            val maxForThisPI = (pa.amountAuthorizedCents - pa.amountCapturedCents).coerceAtLeast(0)
            if (maxForThisPI <= 0) continue

            val amountToCapture = remaining.coerceAtMost(maxForThisPI)
            if (amountToCapture <= 0) continue

            val idemp = "cap-${pa.paymentIntentId}-${UUID.randomUUID()}"
            val cap = paymentService.captureHold(pa.paymentIntentId, amountToCapture, idemp)

            if (cap.ok) {
                pa.amountCapturedCents = (pa.amountCapturedCents + (cap.amountCapturedCents ?: amountToCapture))
                pa.status = AuthStatus.CAPTURED
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
                paymentAuthorizationService.recordCaptureFailureAndMaybeRetry(
                    pa.paymentIntentId, bookingId, userId, customerId, currency, cap
                )
                msgs += "Capture failed for PI ${pa.paymentIntentId}: ${cap.message ?: cap.errorCode}"
            }
        }

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
     * Idempotent: if already canceled/succeeded, we won’t error.
     */
    fun cancelAuthorizationAndUpdate(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        paymentIntentId: String,
        reason: PaymentIntentCancelParams.CancellationReason? = null
    ): CancelAuthorizationResult {
        val pa = authRepo.findByPaymentIntentId(paymentIntentId)

        // If already terminal, short-circuit (saves a Stripe call)
        if (pa != null && (pa.status == AuthStatus.CANCELED || pa.status == AuthStatus.CAPTURED)) {
            updateAggregateState(bookingId, customerId, currency)
            return CancelAuthorizationResult(
                ok = true,
                message = "PaymentIntent already ${pa.status}",
                paymentIntentId = paymentIntentId,
                stripeStatus = pa.status.name.lowercase()
            )
        }

        // Stripe cancel (your PaymentService.cancelHold handles idempotent "already canceled")
        val cancel = paymentService.cancelHold(paymentIntentId, reason)

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
                pa.status = AuthStatus.FAILED
                pa.failureKind = if (cancel.errorType == "api_error" && cancel.canRetry) FailureKind.TRANSIENT else FailureKind.HARD
                pa.lastErrorCode = cancel.errorCode
                pa.lastDeclineCode = cancel.declineCode
                pa.lastErrorMessage = cancel.message
                pa.updatedAt = Instant.now()
                authRepo.save(pa)
            }
        }

        updateAggregateState(bookingId, customerId, currency)

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

    // Optional helper to capture a single PI
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

    private fun cancelAllActiveAuthsForBooking(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String
    ): List<CancelAuthorizationResult> {
        val active = authRepo.findByBookingId(bookingId)
            .filter { it.status == AuthStatus.AUTHORIZED || it.status == AuthStatus.REQUIRES_ACTION || it.status == AuthStatus.REQUIRES_CAPTURE }

        return active.map { pa ->
            cancelAuthorizationAndUpdate(
                bookingId = bookingId,
                userId = userId,
                customerId = customerId,
                currency = currency,
                paymentIntentId = pa.paymentIntentId
            )
        }
    }

}
