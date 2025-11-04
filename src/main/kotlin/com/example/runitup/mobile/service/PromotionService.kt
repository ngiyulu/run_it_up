package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.WaitlistSetupStateRepository
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.push.PaymentPushNotificationService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PromotionService(
    private val sessionRepo: RunSessionRepository,
    private val bookingRepo: BookingRepository,
    private val waitlistSetupRepo: WaitlistSetupStateRepository,
    private val adjuster: BookingPricingAdjuster,
    private val cacheManager: MyCacheManager,
    private val sessionService: RunSessionService,
    private val paymentPushNotificationService: PaymentPushNotificationService
) {
    private val TAG = "PromotionService"

    /**
     * Promote the next waitlisted user if a spot is available.
     * Idempotent by (bookingId + promotionVersion) & Stripe idempotency.
     */
    fun promoteNextWaitlistedUser(sessionId: String): PromotionResult {
        val sessionDb = sessionRepo.findById(sessionId)
            if(!sessionDb.isPresent){
                return PromotionResult(false, "Session not found: $sessionId")
            }
        val session =sessionDb.get()
        if(session.waitList.isEmpty()){
            return PromotionResult(false, "No available spots right now.")
        }
        val candidate = session.waitList[0]
        val booking = bookingRepo.findByUserIdAndRunSessionIdAndStatusIn(
            candidate.userId.orEmpty(), sessionId, mutableListOf(BookingStatus.WAITLISTED)
        ) ?: return PromotionResult(false, "Booking not found")
        val user: User = cacheManager.getUser(candidate.userId.orEmpty()) ?: return PromotionResult(false, "I couldn't find user")
        if(user.stripeId == null){
            return PromotionResult(false, "Stripe id is null")
        }
        // 3) Make a brief reservation/lock on the booking (server-side gate)
        //    Mark as RESERVED_PENDING_AUTH to avoid duplicate promotions in concurrent workers.
        booking.isLocked = true
        booking.isLockedAt = Instant.now()
        bookingRepo.save(booking)

        // 4) Ensure the card is off-session ready (should already be true if you required SCA on waitlist)
        val pmId = booking.paymentMethodId
        if(!session.isSessionFree()){
            if (pmId.isNullOrBlank()) {
                // fallback: you could look up default PM — but best to fail and notify
                // technically this shouldn't happen
                return PromotionResult(false, "No saved payment method for waitlist candidate.", booking.id.orEmpty(), candidate.userId)

            }
            val setup = waitlistSetupRepo.findByBookingIdAndPaymentMethodId(candidate.userId.orEmpty(), pmId)
            if (setup == null || setup.status != SetupStatus.SUCCEEDED) {
                // Not ready for off-session charge. Revert reservation & notify user to finish SCA.
                booking.status = BookingStatus.WAITLISTED
                booking.isLocked = false
                bookingRepo.save(booking)
                paymentPushNotificationService.notifyPaymentActionRequired(candidate.userId.orEmpty(), setup?.setupIntentId.orEmpty())

                return PromotionResult(
                    ok = false,
                    message = "Payment method not off-session ready (SCA required).",
                    bookingId = candidate.userId,
                    userId = candidate.userId,
                    requiresAction = true
                )
            }

            // 5) Create the PRIMARY hold (manual capture) immediately.
            val hold = adjuster.createPrimaryHoldWithChange(
                bookingId = booking.id.orEmpty(),
                userId = candidate.userId.orEmpty(),
                customerId = user.stripeId.orEmpty(),
                currency = "usd",
                totalCents = booking.currentTotalCents,
                savedPaymentMethodId = pmId,
                actorId = "SYSTEM_WAITLIST_PROMOTION"
            )

            // 6) Handle hold result
            if (!hold.ok || hold.paymentIntentId == null) {
                // Primary hold failed. Put booking back to WAITLIST; send notification if needed.
                unlockBooking(booking)

                // If the underlying failure was requires_action, PaymentAuthorizationService already
                // updated PA row; you can notify the user to finish SCA.
                return PromotionResult(
                    ok = false,
                    message = hold.message ?: "Failed to create primary hold.",
                    bookingId = booking.id.orEmpty(),
                    userId = candidate.userId,
                    requiresAction = false // set true only if you surfaced action above
                )
            }
            finishPromotionSuccess(session, user.id.orEmpty(), booking)
            return PromotionResult(
                ok = true,
                message = "User promoted and primary hold authorized.",
                bookingId = booking.id.orEmpty(),
                userId = candidate.userId,
                paymentIntentId = hold.paymentIntentId
            )


        }
        finishPromotionSuccess(session, user.id.orEmpty(), booking)
        return PromotionResult(
            ok = true,
            message = "User promoted and primary hold authorized.",
            bookingId = booking.id.orEmpty(),
            userId = candidate.userId,
            paymentIntentId = null
        )

    }

    private fun unlockBooking(booking: Booking){
        booking.status = BookingStatus.WAITLISTED
        booking.isLocked = false
        bookingRepo.save(booking)
    }
    private fun finishPromotionSuccess(runSession: RunSession, userId:String, booking: Booking){
        runSession.waitList.removeAll {
            it.userId == userId
        }
        sessionService.updateRunSession(runSession)
        booking.status = BookingStatus.JOINED
        booking.promotedAt = Instant.now()
        booking.isLocked = false
        bookingRepo.save(booking)
        //TODO: add text message and email

    }
}
data class PromotionResult(
    val ok: Boolean,
    val message: String? = null,
    val bookingId: String? = null,
    val userId: String? = null,
    val paymentIntentId: String? = null,
    val requiresAction: Boolean = false
)