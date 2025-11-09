package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.WaitlistSetupStateRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.push.PaymentPushNotificationService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PromotionService(
    private val bookingRepo: BookingRepository,
    private val waitlistSetupRepo: WaitlistSetupStateRepository,
    private val adjuster: BookingPricingAdjuster,
    private  val bookingDbService: BookingDbService,
    private val cacheManager: MyCacheManager,
    private val sessionService: RunSessionService,
    private val paymentPushNotificationService: PaymentPushNotificationService
) {
    val logger = myLogger()

    /**
     * Promote the next waitlisted user if a spot is available.
     * - Skips entries whose bookings are currently locked by another worker.
     * - Continues scanning until a promotable candidate is found or the list is exhausted.
     * Idempotent by (bookingId + promotionVersion) & Stripe idempotency (in adjuster).
     */
    fun promoteNextWaitlistedUser(sessionId: String): PromotionResult {
        val session = cacheManager.getRunSession(sessionId)
            ?: return PromotionResult(false, "Session not found: $sessionId")

        if (session.waitList.isEmpty()) {
            return PromotionResult(false, "No available spots right now.")
        }

        // Snapshot so we can iterate safely even if we mutate session later
        val candidates = session.waitList.toList()

        for (candidate in candidates) {
            val userId = candidate.userId.orEmpty()
            // Find a WAITLISTED booking for this candidate
            val booking = bookingRepo.findByUserIdAndRunSessionIdAndStatusIn(
                userId, sessionId, mutableListOf(BookingStatus.WAITLISTED)
            ) ?: continue // no booking; try next

            // If already locked or cannot lock now, skip to next
            if (booking.isLocked || !tryLockBooking(booking)) {
                logger.info("booking is already locked so we are skipping")
                continue
            }

            try {
                val user: User = cacheManager.getUser(userId)
                    ?: return PromotionResult(false, "I couldn't find user")

                if (user.stripeId == null) {
                    unlockBooking(booking)
                    return PromotionResult(false, "Stripe id is null")
                }

                // If session is free (no payment required), just promote
                if (session.isSessionFree()) {
                    finishPromotionSuccess(session, user.id.orEmpty(), booking)
                    return PromotionResult(
                        ok = true,
                        message = "User promoted (free session).",
                        bookingId = booking.id.orEmpty(),
                        userId = userId,
                        paymentIntentId = null
                    )
                }

                // Paid session: require off-session-ready payment method
                val pmId = booking.paymentMethodId
                if (pmId.isNullOrBlank()) {
                    unlockBooking(booking)
                    return PromotionResult(
                        ok = false,
                        message = "No saved payment method for waitlist candidate.",
                        bookingId = booking.id.orEmpty(),
                        userId = userId
                    )
                }

                val setup = waitlistSetupRepo.findByBookingIdAndPaymentMethodId(userId, pmId)
                if (setup == null || setup.status != SetupStatus.SUCCEEDED) {
                    // Not ready for off-session charge. Revert & notify.
                    booking.status = BookingStatus.WAITLISTED
                    booking.isLocked = false
                    bookingRepo.save(booking)
                    paymentPushNotificationService.notifyPaymentActionRequired(userId, setup?.setupIntentId.orEmpty())

                    return PromotionResult(
                        ok = false,
                        message = "Payment method not off-session ready (SCA required).",
                        bookingId = booking.id.orEmpty(),
                        userId = userId,
                        requiresAction = true
                    )
                }

                // Create the PRIMARY hold (manual capture)
                val hold = adjuster.createPrimaryHoldWithChange(
                    bookingId = booking.id.orEmpty(),
                    userId = userId,
                    customerId = user.stripeId.orEmpty(),
                    currency = "usd",
                    totalCents = booking.currentTotalCents,
                    savedPaymentMethodId = pmId,
                    actorId = "SYSTEM_WAITLIST_PROMOTION"
                )

                if (!hold.ok || hold.paymentIntentId == null) {
                    unlockBooking(booking)
                    // Move to next candidate if hold failed (don’t abort the whole loop)
                    continue
                }

                finishPromotionSuccess(session, user.id.orEmpty(), booking)
                return PromotionResult(
                    ok = true,
                    message = "User promoted and primary hold authorized.",
                    bookingId = booking.id.orEmpty(),
                    userId = userId,
                    paymentIntentId = hold.paymentIntentId
                )
            } catch (ex: Exception) {
                logger.error("promoteNextWaitlistedUser failed $ex")
                // Safety: In case anything above throws, unlock so others can retry later
                unlockBooking(booking)
                // Try the next candidate instead of failing the whole operation
                continue
            }
        }
        // If we got here, nobody in the waitlist could be promoted right now
        return PromotionResult(false, "No promotable candidate at this time (all locked or invalid).")
    }

    /**
     * Try to acquire a server-side lock on the booking.
     * Prefer an atomic DB update; fall back to optimistic-locking save if needed.
     */
    private fun tryLockBooking(booking: Booking): Boolean {
        val now = Instant.now()
        // Attempt atomic DB-level lock (preferred)
        return try {
            val modified = bookingDbService.tryLock(booking.id.orEmpty(), now)// Someone else locked it first
            // Successfully acquired the lock
            modified == 1
        } catch (ex: Exception) {
            logger.error("tryLockBooking failed $ex")
            // Fallback to safe mode — only if something unexpected happened with Mongo
            try {
                if (booking.isLocked) return false
                booking.isLocked = true
                booking.isLockedAt = now
                bookingRepo.save(booking)
                true
            } catch (e: Exception) {
                logger.error("tryLockBooking failed $e")
                false
            }
        }
    }


    private fun unlockBooking(booking: Booking) {
        booking.status = BookingStatus.WAITLISTED
        booking.isLocked = false
        bookingRepo.save(booking)
    }

    private fun finishPromotionSuccess(runSession: RunSession, userId: String, booking: Booking) {
        runSession.waitList.removeAll { it.userId == userId }
        sessionService.updateRunSession(runSession)

        booking.status = BookingStatus.JOINED
        booking.promotedAt = Instant.now()
        booking.isLocked = false
        bookingRepo.save(booking)
        // TODO: add text message and email
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
