// src/test/kotlin/com/example/runitup/mobile/service/PromotionServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.WaitlistSetupStateRepository
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.push.PaymentPushNotificationService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class PromotionServiceTest {

    private val sessionRepo = mockk<RunSessionRepository>()
    private val bookingRepo = mockk<BookingRepository>(relaxed = true)
    private val waitlistSetupRepo = mockk<WaitlistSetupStateRepository>()
    private val adjuster = mockk<BookingPricingAdjuster>()
    private val cacheManager = mockk<MyCacheManager>()
    private val sessionService = mockk<RunSessionService>(relaxed = true)
    private val pushSvc = mockk<PaymentPushNotificationService>(relaxed = true)

    private lateinit var service: PromotionService

    @BeforeEach
    fun setup() {
        service = PromotionService(
            sessionRepo, bookingRepo, waitlistSetupRepo,
            adjuster, cacheManager, sessionService, pushSvc
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    // ---------- Helpers ----------
    private fun sessionWith(
        id: String = "s1",
        amount: Double = 0.0, // free by default
        waitUsers: MutableList<RunUser> = mutableListOf(),
        maxPlayers: Int = 10
    ): RunSession {
        return RunSession(
            id = id,
            gym = null,
            location = null,
            date = LocalDate.now(),
            startTime = LocalTime.NOON,
            endTime = LocalTime.NOON.plusHours(2),
            zoneId = "America/Chicago",
            startAtUtc = null,
            hostedBy = null,
            host = null,
            allowGuest = true,
            duration = 2,
            notes = "",
            privateRun = false,
            description = "",
            amount = amount,
            total = 0.0,
            maxPlayer = maxPlayers,
            title = "Run",
            bookings = mutableListOf(),
            waitList = waitUsers,
            players = mutableListOf(),
            maxGuest = 2,
            isFree = amount == 0.0,
            isFull = false
        )
    }

    private fun booking(
        id: String = "b1",
        userId: String = "u1",
        status: BookingStatus = BookingStatus.WAITLISTED,
        paymentMethodId: String? = null,
        totalCents: Long = 1500
    ) = Booking(
        id = id,
        runSessionId = "s1",
        userId = userId,
        status = status,
        paymentMethodId = paymentMethodId,
        currentTotalCents = totalCents,
        joinedAtFromWaitList = Instant.now(),
        sessionAmount = 0.0,
        total = 0.0,
        user = RunUser()
    )

    private fun user(
        id: String = "u1",
        stripeId: String? = "cus_123"
    ) = User(
        id = id, firstName = "F", lastName = "L",
        stripeId = stripeId
    )

    // ---------- Tests ----------

    @Test
    fun `returns not found when session is missing`() {
        every { sessionRepo.findById("missing") } returns Optional.empty()

        val res = service.promoteNextWaitlistedUser("missing")

        assertThat(res.ok).isFalse()
        assertThat(res.message).contains("Session not found")
        verify(exactly = 0) { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn(any(), any(), any()) }
    }

    @Test
    fun `returns message when waitlist empty`() {
        val session = sessionWith(id = "s1", waitUsers = mutableListOf())
        every { sessionRepo.findById("s1") } returns Optional.of(session)

        val res = service.promoteNextWaitlistedUser("s1")

        assertThat(res.ok).isFalse()
        assertThat(res.message).isEqualTo("No available spots right now.")
    }

    @Test
    fun `booking not found for first waitlisted user`() {
        val session = sessionWith(id = "s1", waitUsers = mutableListOf(RunUser(userId = "u1")))
        every { sessionRepo.findById("s1") } returns Optional.of(session)
        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u1", "s1", any())
        } returns null

        val res = service.promoteNextWaitlistedUser("s1")

        assertThat(res.ok).isFalse()
        assertThat(res.message).isEqualTo("Booking not found")
    }

    @Test
    fun `user not in cache`() {
        val session = sessionWith(id = "s1", waitUsers = mutableListOf(RunUser(userId = "u1")))
        val bk = booking(userId = "u1", paymentMethodId = "pm_x")
        every { sessionRepo.findById("s1") } returns Optional.of(session)
        every { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u1", "s1", any()) } returns bk
        every { cacheManager.getUser("u1") } returns null

        val res = service.promoteNextWaitlistedUser("s1")

        assertThat(res.ok).isFalse()
        assertThat(res.message).isEqualTo("I couldn't find user")
    }

    @Test
    fun `user has null stripeId`() {
        val session = sessionWith(id = "s1", waitUsers = mutableListOf(RunUser(userId = "u1")))
        val bk = booking(userId = "u1", paymentMethodId = "pm_x")
        every { sessionRepo.findById("s1") } returns Optional.of(session)
        every { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u1", "s1", any()) } returns bk
        every { cacheManager.getUser("u1") } returns user(stripeId = null)

        val res = service.promoteNextWaitlistedUser("s1")

        assertThat(res.ok).isFalse()
        assertThat(res.message).isEqualTo("Stripe id is null")
    }

    @Test
    fun `not free - missing payment method returns early with message`() {
        val session = sessionWith(id = "s1", amount = 10.0, waitUsers = mutableListOf(RunUser(userId = "u1")))
        val bk = booking(userId = "u1", paymentMethodId = null) // missing PM
        every { sessionRepo.findById("s1") } returns Optional.of(session)
        every { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u1", "s1", any()) } returns bk
        every { bookingRepo.save(any()) } answers { firstArg() } // save lock
        every { cacheManager.getUser("u1") } returns user(stripeId = "cus_123")

        val res = service.promoteNextWaitlistedUser("s1")

        assertThat(res.ok).isFalse()
        assertThat(res.message).contains("No saved payment method")
        // booking should have been locked before the check
        assertThat(bk.isLocked).isTrue()
        verify { bookingRepo.save(bk) } // lock save
        // no calls beyond this
        verify(exactly = 0) { waitlistSetupRepo.findByBookingIdAndPaymentMethodId(any(), any()) }
    }

    @Test
    fun `not free - setup missing or not succeeded reverts booking and notifies`() {
        val session = sessionWith(id = "s1", amount = 10.0, waitUsers = mutableListOf(RunUser(userId = "u1")))
        val bk = booking(id = "b1", userId = "u1", paymentMethodId = "pm_x")
        every { sessionRepo.findById("s1") } returns Optional.of(session)
        every { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u1", "s1", any()) } returns bk
        every { bookingRepo.save(any()) } answers { firstArg() }
        every { cacheManager.getUser("u1") } returns user(id = "u1", stripeId = "cus_123")

        // Simulate: setup not found or not SUCCEEDED
        every { waitlistSetupRepo.findByBookingIdAndPaymentMethodId(any(), any()) } returns null
        every { pushSvc.notifyPaymentActionRequired("u1", any()) } just Runs

        val res = service.promoteNextWaitlistedUser("s1")

        assertThat(res.ok).isFalse()
        assertThat(res.requiresAction).isTrue()
        assertThat(bk.status).isEqualTo(BookingStatus.WAITLISTED)
        assertThat(bk.isLocked).isFalse()

        // Ensure revert and notify happened
        verify(atLeast = 2) { bookingRepo.save(bk) }  // once for lock, once for revert
        verify(exactly = 1) { pushSvc.notifyPaymentActionRequired("u1", any()) }
        // No adjuster call since setup not ready
        verify(exactly = 0) { adjuster.createPrimaryHoldWithChange(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `free session - booking promoted, waitlist pruned, session updated`() {
        val ru = RunUser(userId = "u1")
        val session = sessionWith(id = "s1", amount = 0.0, waitUsers = mutableListOf(ru))
        val bk = booking(id = "b1", userId = "u1", paymentMethodId = null)

        every { sessionRepo.findById("s1") } returns Optional.of(session)
        every { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u1", "s1", any()) } returns bk
        every { bookingRepo.save(any()) } answers { firstArg() }
        every { cacheManager.getUser("u1") } returns user(id = "u1", stripeId = "cus_123") // stripeId irrelevant for free

        // Act
        val res = service.promoteNextWaitlistedUser("s1")

        // Assert result
        assertThat(res.ok).isTrue()
        assertThat(res.paymentIntentId).isNull()

        // booking state updated
        assertThat(bk.status).isEqualTo(BookingStatus.JOINED)
        assertThat(bk.isLocked).isFalse()
        assertThat(bk.promotedAt).isNotNull()

        // session waitlist updated & persisted
        assertThat(session.waitList.any { it.userId == "u1" }).isFalse()
        verify(exactly = 1) { sessionService.updateRunSession(session) }

        // no adjuster invoked on free sessions
        verify(exactly = 0) { adjuster.createPrimaryHoldWithChange(any(), any(), any(), any(), any(), any(), any()) }
    }
}
