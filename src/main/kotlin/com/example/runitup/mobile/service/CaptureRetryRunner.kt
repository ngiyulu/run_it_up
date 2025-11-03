import com.example.runitup.mobile.model.AuthStatus
import com.example.runitup.mobile.model.FailureKind
import com.example.runitup.mobile.repository.PaymentAuthorizationRepository
import com.example.runitup.mobile.service.PaymentAuthorizationService
import com.example.runitup.mobile.service.PaymentService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CaptureRetryRunner(
    private val authRepo: PaymentAuthorizationRepository,
    private val paymentService: PaymentService,
    private val paymentAuthorizationService: PaymentAuthorizationService
) {
    // @Scheduled(fixedDelay = 60_000)
    fun run() {
        val now = System.currentTimeMillis()
        val candidates = authRepo.findCaptureRetryCandidates(now)

        candidates.forEach { pa ->
            try {
                val idemp = "capture-retry-${pa.paymentIntentId}-${pa.retryCount + 1}"
                val cap = paymentService.captureHold(
                    paymentIntentId = pa.paymentIntentId,
                    captureAmountCents = pa.amountAuthorizedCents, // or the final amount for this PI
                    idempotencyKey = idemp
                )

                if (cap.ok) {
                    // success: mark CAPTURED and clear failure fields
                    pa.status = AuthStatus.CAPTURED
                    pa.amountCapturedCents = cap.amountCapturedCents ?: pa.amountAuthorizedCents
                    pa.failureKind = FailureKind.NONE
                    pa.lastErrorCode = null
                    pa.lastDeclineCode = null
                    pa.lastErrorMessage = null
                    pa.nextRetryAt = null
                    pa.updatedAt = Instant.now()
                    authRepo.save(pa)
                    // also update your BookingPaymentState aggregates here if desired
                } else {
                    // failed again → schedule (or stop if reached 2 tries)
                    paymentAuthorizationService.recordCaptureFailureAndMaybeRetry(
                        paymentIntentId = pa.paymentIntentId,
                        bookingId = pa.bookingId,
                        userId = pa.userId,
                        customerId = pa.customerId,
                        currency = pa.currency,
                        result = cap
                    )
                }
            } catch (e: Exception) {
                // swallow and let next run handle
            }
        }
    }
}
