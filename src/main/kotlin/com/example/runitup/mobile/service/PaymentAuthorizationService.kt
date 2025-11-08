package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.repository.PaymentFailureLogRepository
import com.example.runitup.mobile.rest.v1.dto.payment.CreateChargeResult
import com.example.runitup.mobile.service.push.PaymentPushNotificationService
import com.example.runitup.mobile.utility.CaptureFailurePolicy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentAuthorizationService {

    @Autowired
    lateinit var authRepo: PaymentAuthorizationRepository

    @Autowired
    lateinit var failureLogRepo: PaymentFailureLogRepository

    @Autowired
    lateinit var pushService: PaymentPushNotificationService

    @Autowired
    lateinit var mongoTemplate: MongoTemplate
    fun upsertPaymentAuthorizationFromCreate(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        role: AuthRole,                           // PRIMARY or DELTA
        changeEventId: String?,
        create: CreateChargeResult,               // from your PaymentService.createCharge(...)
    ): PaymentAuthorization {
        require(!create.paymentIntentId.isNullOrBlank()) { "PI id required" }

        val status = when {
            create.status == "requires_action" || create.requiresAction -> AuthStatus.REQUIRES_ACTION
            create.status == "requires_capture" || create.status == "processing" -> AuthStatus.AUTHORIZED
            create.status == "succeeded" -> AuthStatus.CAPTURED
            create.status == "canceled" -> AuthStatus.CANCELED
            else -> AuthStatus.AUTHORIZED
        }

        val existing = authRepo.findByPaymentIntentId(create.paymentIntentId)
        val now = Instant.now()

        val row = (existing ?: PaymentAuthorization(
            bookingId = bookingId,
            userId = userId,
            customerId = customerId,
            paymentIntentId = create.paymentIntentId,
            role = role,
            amountAuthorizedCents = create.amountCents ?: 0L,
            currency = currency,
            status = status,
            relatedChangeEventId = changeEventId,
            createdAt = now,
            updatedAt = now,
            note = create.message ?: create.lastPaymentErrorMessage
        )).copy(
            amountAuthorizedCents = create.amountCents ?: existing?.amountAuthorizedCents ?: 0L,
            status = status,
            updatedAt = now,
            note = create.message ?: create.lastPaymentErrorMessage ?: existing?.note
        )
        return if (existing == null) authRepo.insert(row) else authRepo.save(row)
    }

    fun recordCreateFailureNoRetry(
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        role: AuthRole,
        changeEventId: String?,
        result: CreateChargeResult
    ) {
        val now = Instant.now()

        val pa = PaymentAuthorization(
            bookingId = bookingId,
            userId = userId,
            customerId = customerId,
            paymentIntentId = result.paymentIntentId ?: "unknown",
            role = role,
            amountAuthorizedCents = result.amountCents ?: 0L,
            currency = currency,
            status = if (result.requiresAction || result.status == "requires_action")
                AuthStatus.REQUIRES_ACTION else AuthStatus.FAILED,
            relatedChangeEventId = changeEventId
        ).apply {
            lastOperation = LastOperation.CREATE
            failureKind = if (result.requiresAction) FailureKind.ACTION_REQUIRED else FailureKind.HARD
            lastErrorCode = result.errorCode ?: result.lastPaymentErrorCode
            lastDeclineCode = result.declineCode ?: result.lastPaymentErrorDeclineCode
            lastErrorMessage = result.message ?: result.lastPaymentErrorMessage
            lastFailureAt = now
            retryCount = 0
            nextRetryAt = null            // <-- no retry for holds
            maxRetries = 2
            needsUserAction = (status == AuthStatus.REQUIRES_ACTION)
            updatedAt = now
        }

        authRepo.insert(pa)

        // Optional: push user if action required
        if (pa.needsUserAction && pa.notifiedUserActionAt == null) {
            pushService.notifyPaymentActionRequired(userId, pa.paymentIntentId)
            pa.notifiedUserActionAt = now
            authRepo.save(pa)
        }
        // Also append an immutable failure log row (omitted here for brevity)
    }


    fun recordCaptureFailureAndMaybeRetry(
        paymentIntentId: String,
        bookingId: String,
        userId: String,
        customerId: String,
        currency: String,
        result: CaptureHoldResult
    ) {
        val now = Instant.now()
        val pa = authRepo.findByPaymentIntentId(paymentIntentId)
            ?: return // nothing to update; you can also create a minimal row

        val nextAttempt = pa.retryCount + 1
        val decision = CaptureFailurePolicy.decide(nextAttempt)

        pa.apply {
            status = AuthStatus.FAILED
            lastOperation = LastOperation.CAPTURE
            failureKind = if (result.errorType == "api_error" || result.canRetry) FailureKind.TRANSIENT else FailureKind.HARD
            lastErrorCode = result.errorCode
            lastDeclineCode = result.declineCode
            lastErrorMessage = result.message
            lastFailureAt = now
            retryCount = nextAttempt
            nextRetryAt = if (decision.shouldRetry) System.currentTimeMillis() + decision.backoffMs else null
            updatedAt = now
        }
        authRepo.save(pa)

        // Append immutable log (optional)
        failureLogRepo.insert(
            PaymentFailureLog(
                bookingId = bookingId,
                paymentIntentId = paymentIntentId,
                userId = userId,
                eventType = "CAPTURE_PI",
                failureKind = pa.failureKind,
                errorCode = pa.lastErrorCode,
                declineCode = pa.lastDeclineCode,
                message = pa.lastErrorMessage,
                attempt = nextAttempt
            )
        )
    }
}