package com.example.runitup.mobile.service.payment

import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.repository.RefundRecordRepository
import com.example.runitup.mobile.rest.v1.dto.payment.RefundResult
import com.stripe.exception.CardException
import com.stripe.exception.StripeException
import com.stripe.model.Refund
import com.stripe.net.RequestOptions
import com.stripe.param.RefundCreateParams
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import kotlin.math.min


@Service
class RefundService(
    private val authRepo: PaymentAuthorizationRepository,
    private val refundRepo: RefundRecordRepository,
    private val bookingStateRepo: BookingPaymentStateRepository
): BasePaymentService(authRepo, bookingStateRepo){

    @Autowired
    lateinit var refundPolicyService: RefundPolicyService

    fun startRefund(runSession: RunSession, booking: BookingPaymentState){
        val decision = refundPolicyService.computeRefundForCancellation(runSession, chargeCents = booking.totalCapturedCents)
        if (decision.eligible && decision.refundCents > 0) {
            // call your RefundService to issue a Stripe refund for decision.refundCents
            // and persist a Refund record (amount, reason, timestamps, PA/charge linkage)
        } else {
            // no refund – surface decision.reason to the client
        }
    }

    /**
     * Compute how much is still refundable for a booking, based on PaymentAuthorization rows.
     * refundable = sum(captured) - sum(refunded)
     */
    fun refundableRemainingForBooking(bookingId: String): Long {
        val auths = authRepo.findByBookingId(bookingId)
        val captured = auths.sumOf { it.amountCapturedCents }
        val refunded = auths.sumOf { it.amountRefundedCents }
        return (captured - refunded).coerceAtLeast(0)
    }

    /**
     * Issue a refund against a specific PaymentIntent (partial or full).
     * Updates:
     *  - RefundRecord (audit)
     *  - PaymentAuthorization.amountRefundedCents
     *  - BookingPaymentState totals
     */
    fun refundPaymentIntent(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        paymentIntentId: String,
        amountCents: Long, // must be >0 and <= refundable
        reasonCode: RefundReasonCode,
        reasonMessage: String? = null,
        idempotencyKey: String = "rf-$paymentIntentId-${UUID.randomUUID()}"
    ): RefundResult {
        if (amountCents <= 0) {
            return RefundResult(
                ok = false, bookingId = bookingId, userId = userId, paymentIntentId = paymentIntentId,
                amountCents = 0, message = "Refund amount must be > 0"
            )
        }

        // Enforce "not more than still refundable"
        val refundableRemaining = refundableRemainingForBooking(bookingId)
        if (refundableRemaining <= 0) {
            return RefundResult(
                ok = false, bookingId = bookingId, userId = userId, paymentIntentId = paymentIntentId,
                amountCents = 0, message = "Nothing left to refund."
            )
        }
        val finalAmount = min(amountCents, refundableRemaining)

        // Create local audit record (REQUESTED)
        val record = RefundRecord(
            bookingId = bookingId,
            userId = userId,
            customerId = customerId,
            currency = currency,
            paymentIntentId = paymentIntentId,
            amountCents = finalAmount,
            status = RefundStatus.REQUESTED,
            reasonCode = reasonCode,
            reasonMessage = reasonMessage,
            idempotencyKey = idempotencyKey
        )
        refundRepo.save(record)

        // Call Stripe
        return try {
            val params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(finalAmount)
                .apply {
                    // optional: map certain reason codes to Stripe's "reason" when appropriate
                    // setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                }
                .putMetadata("bookingId", bookingId)
                .putMetadata("userId", userId)
                .putMetadata("app_reason_code", reasonCode.name)
                .apply { reasonMessage?.let { putMetadata("app_reason_message", it) } }
                .build()

            val opts = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build()
            val stripeRefund: Refund = Refund.create(params, opts)

            // Update local audit
            record.status = RefundStatus.SUCCEEDED
            record.stripeRefundId = stripeRefund.id
            record.updatedAt = Instant.now()
            refundRepo.save(record)

            // Update PaymentAuthorization accounting (by PI)
            val pa = authRepo.findByPaymentIntentId(paymentIntentId)
            if (pa != null) {
                pa.amountRefundedCents = (pa.amountRefundedCents + finalAmount)
                pa.updatedAt = Instant.now()
                authRepo.save(pa)
            }

            // Update booking payment state aggregates
            updateAggregateState(bookingId, customerId, currency)

            RefundResult(
                ok = true,
                bookingId = bookingId,
                userId = userId,
                paymentIntentId = paymentIntentId,
                stripeRefundId = stripeRefund.id,
                amountCents = finalAmount,
                message = "Refund succeeded."
            )
        } catch (e: CardException) {
            val se = e.stripeError
            record.status = RefundStatus.FAILED
            record.errorType = se?.type ?: "card_error"
            record.errorCode = e.code
            record.declineCode = se?.declineCode
            record.errorMessage = se?.message ?: e.message
            record.updatedAt = Instant.now()
            refundRepo.save(record)

            RefundResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                paymentIntentId = paymentIntentId,
                amountCents = finalAmount,
                message = se?.message ?: e.message,
                errorType = se?.type ?: "card_error",
                errorCode = e.code,
                declineCode = se?.declineCode
            )
        } catch (e: StripeException) {
            record.status = RefundStatus.FAILED
            record.errorType = e.stripeError?.type ?: "api_error"
            record.errorCode = e.code
            record.declineCode = e.stripeError?.declineCode
            record.errorMessage = e.message
            record.updatedAt = Instant.now()
            refundRepo.save(record)

            RefundResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                paymentIntentId = paymentIntentId,
                amountCents = finalAmount,
                message = e.message,
                errorType = e.stripeError?.type ?: "api_error",
                errorCode = e.code,
                declineCode = e.stripeError?.declineCode
            )
        } catch (e: Exception) {
            record.status = RefundStatus.FAILED
            record.errorType = "internal_error"
            record.errorMessage = e.message
            record.updatedAt = Instant.now()
            refundRepo.save(record)

            RefundResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                paymentIntentId = paymentIntentId,
                amountCents = finalAmount,
                message = "Unexpected error: ${e.message}",
                errorType = "internal_error"
            )
        }
    }

    /**
     * Refund across a booking without specifying which PI.
     * Strategy: refund against the most-recent captured authorizations first (LIFO),
     * until amount is satisfied.
     */
    fun refundBooking(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        amountCents: Long,
        reasonCode: RefundReasonCode,
        reasonMessage: String? = null
    ): RefundResult {
        if (amountCents <= 0) {
            return RefundResult(false, bookingId, userId, amountCents = 0, message = "Refund amount must be > 0")
        }

        var remaining = min(amountCents, refundableRemainingForBooking(bookingId))
        if (remaining <= 0) {
            return RefundResult(false, bookingId, userId, amountCents = 0, message = "Nothing left to refund.")
        }

        // Find captured PAs (capture-first order; you can switch to FIFO if you prefer)
        val captured = authRepo.findByBookingIdOrderByCreatedAtDesc(bookingId)
            .filter { it.amountCapturedCents > it.amountRefundedCents }

        val messages = mutableListOf<String>()
        var lastOk = true
        var lastResult: RefundResult? = null

        for (pa in captured) {
            if (remaining <= 0) break
            val available = (pa.amountCapturedCents - pa.amountRefundedCents).coerceAtLeast(0)
            if (available <= 0) continue

            val take = min(available, remaining)

            val res = refundPaymentIntent(
                bookingId = bookingId,
                userId = userId,
                customerId = customerId,
                currency = currency,
                paymentIntentId = pa.paymentIntentId,
                amountCents = take,
                reasonCode = reasonCode,
                reasonMessage = reasonMessage,
                idempotencyKey = "rf-$bookingId-${pa.paymentIntentId}-${UUID.randomUUID()}"
            )

            messages += (res.message ?: if (res.ok) "Refunded $take¢" else "Refund failed for ${pa.paymentIntentId}")
            lastOk = res.ok
            lastResult = res
            if (!res.ok) break

            remaining -= take
        }

        // After loop, update aggregate state once more in case multiple PIs were refunded
        val bookingState = bookingStateRepo.findByBookingId(bookingId)
        if (bookingState != null) {
            // you might keep totalRefundedCents inside BookingPaymentState too
            bookingState.refundableRemainingCents = refundableRemainingForBooking(bookingId)
            bookingState.latestUpdatedAt = Instant.now()
            bookingStateRepo.save(bookingState)
        }

        return if (lastOk && remaining == 0L) {
            RefundResult(
                ok = true,
                bookingId = bookingId,
                userId = userId,
                paymentIntentId = lastResult?.paymentIntentId,
                stripeRefundId = lastResult?.stripeRefundId,
                amountCents = amountCents,
                message = "Refunded $amountCents¢ across booking."
            )
        } else {
            RefundResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                paymentIntentId = lastResult?.paymentIntentId,
                amountCents = amountCents - remaining,
                message = "Partial/failed booking refund. Remaining not refunded: $remaining¢",
                errorType = lastResult?.errorType,
                errorCode = lastResult?.errorCode,
                declineCode = lastResult?.declineCode
            )
        }
    }

    /**
     * Convenience: Apply policy and refund accordingly.
     * Pass the captured total for the booking (or compute from BookingPaymentState).
     */
    fun applyPolicyAndRefund(
        run: RunSession,
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        capturedTotalCents: Long,
        reasonMessage: String? = null
    ): RefundResult {
        val policy = RefundPolicyService().computeRefundForCancellation(run, capturedTotalCents)
        if (!policy.eligible || policy.refundCents <= 0) {
            return RefundResult(
                ok = false,
                bookingId = bookingId,
                userId = userId,
                amountCents = 0,
                message = policy.reason
            )
        }
        return refundBooking(
            bookingId = bookingId,
            userId = userId,
            customerId = customerId,
            currency = currency,
            amountCents = policy.refundCents,
            reasonCode = RefundReasonCode.USER_CANCELLED,
            reasonMessage = reasonMessage ?: policy.reason
        )
    }


}