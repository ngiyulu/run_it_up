package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.WaitlistSetupStateRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.payment.PrimaryHoldResult
import com.example.runitup.mobile.service.push.PaymentPushNotificationService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PromotionServiceTest {

    private val bookingRepo = mockk<BookingRepository>(relaxed = true)
    private val waitlistSetupRepo = mockk<WaitlistSetupStateRepository>()
    private val bookingDbService = mockk<BookingDbService>()
    private val adjuster = mockk<BookingPricingAdjuster>()
    private val cacheManager = mockk<MyCacheManager>()
    private val runSessionPushNotificationService = mockk<RunSessionPushNotificationService>(relaxed = true)
    private val sessionService = mockk<RunSessionService>(relaxed = true)
    private val runSessionEventLogger = mockk<RunSessionEventLogger>(relaxed = true)
    private val paymentPushNotificationService = mockk<PaymentPushNotificationService>(relaxed = true)

    private lateinit var service: PromotionService

    @BeforeEach
    fun setup() {
        service = PromotionService(
            bookingRepo = bookingRepo,
            waitlistSetupRepo = waitlistSetupRepo,
            adjuster = adjuster,
            bookingDbService = bookingDbService,
            cacheManager = cacheManager,
            sessionService = sessionService,
            paymentPushNotificationService = paymentPushNotificationService,
            runSessionEventLogger = runSessionEventLogger,
            runSessionPushNotificationService = runSessionPushNotificationService
        )

        // Avoid ClassCastException for save()
        every { bookingRepo.save(any<Booking>()) } answers { firstArg() }
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    // ---------------------------------------------------------
    // 1) Session missing
    // ---------------------------------------------------------
    @Test
    fun `session not found`() {
        every { cacheManager.getRunSession("sess-1") } returns null

        val result = service.promoteNextWaitlistedUser("sess-1")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("Session not found: sess-1")

        verify(exactly = 1) { cacheManager.getRunSession("sess-1") }
        verify(exactly = 0) { bookingRepo.findByUserIdAndRunSessionIdAndStatusIn(any(), any(), any()) }
    }

    // ---------------------------------------------------------
    // 2) Empty waitlist
    // ---------------------------------------------------------
    @Test
    fun `empty waitlist`() {
        val session = mockk<RunSession>()
        every { session.waitList } returns mutableListOf()
        every { cacheManager.getRunSession("sess-2") } returns session

        val result = service.promoteNextWaitlistedUser("sess-2")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("No available spots right now.")

        verify(exactly = 1) { cacheManager.getRunSession("sess-2") }
        verify(exactly = 0) { bookingRepo.save(any()) }
    }

    // ---------------------------------------------------------
    // 3) Candidate has no booking
    // ---------------------------------------------------------
    @Test
    fun `candidate has no booking`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-1"

        val session = mockk<RunSession>()
        every { session.waitList } returns mutableListOf(runUser)
        every { cacheManager.getRunSession("sess-3") } returns session

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-1", "sess-3", any())
        } returns null

        val result = service.promoteNextWaitlistedUser("sess-3")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("No promotable candidate at this time (all locked or invalid).")

        verify(exactly = 1) {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-1", "sess-3", any())
        }
        verify(exactly = 0) { bookingRepo.save(any()) }
    }

    // ---------------------------------------------------------
    // 4) Booking cannot be locked
    // ---------------------------------------------------------
    @Test
    fun `booking cannot be locked`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-2"

        val session = mockk<RunSession>()
        every { session.waitList } returns mutableListOf(runUser)
        every { cacheManager.getRunSession("sess-4") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-1"

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-2", "sess-4", any())
        } returns booking

        every { bookingDbService.tryLock("b-1", any()) } returns 0 // cannot lock

        val result = service.promoteNextWaitlistedUser("sess-4")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("No promotable candidate at this time (all locked or invalid).")

        verify(exactly = 1) { bookingDbService.tryLock("b-1", any()) }
        verify(exactly = 0) { bookingRepo.save(any()) }
    }

    // ---------------------------------------------------------
    // 5) User not found
    // ---------------------------------------------------------
    @Test
    fun `user not found`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-3"

        val session = mockk<RunSession>()
        every { session.waitList } returns mutableListOf(runUser)
        every { cacheManager.getRunSession("sess-5") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-2"

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-3", "sess-5", any())
        } returns booking

        every { bookingDbService.tryLock("b-2", any()) } returns 1
        every { cacheManager.getUser("u-3") } returns null

        val result = service.promoteNextWaitlistedUser("sess-5")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("I couldn't find user")

        verify(exactly = 1) { cacheManager.getUser("u-3") }
        verify(exactly = 0) { bookingRepo.save(any()) }
    }

    // ---------------------------------------------------------
    // 6) Missing Stripe ID → unlock
    // ---------------------------------------------------------
    @Test
    fun `missing stripeId`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-4"

        val session = mockk<RunSession>()
        every { session.waitList } returns mutableListOf(runUser)
        every { cacheManager.getRunSession("sess-6") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-3"

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-4", "sess-6", any())
        } returns booking

        every { bookingDbService.tryLock("b-3", any()) } returns 1

        val user = mockk<User>()
        every { user.id } returns "u-4"
        every { user.stripeId } returns null

        every { cacheManager.getUser("u-4") } returns user

        val result = service.promoteNextWaitlistedUser("sess-6")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("Stripe id is null")

        verify(exactly = 1) { bookingRepo.save(booking) }
    }

    // ---------------------------------------------------------
    // 7) Free session → success
    // ---------------------------------------------------------
    @Test
    fun `free session promotes successfully`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-5"

        // Real lists the service can mutate
        val waitList = mutableListOf(runUser)
        val bookingList = mutableListOf<RunSession.SessionRunBooking>()

        val session = mockk<RunSession>(relaxed = true)
        every { session.id } returns "sess-7"
        every { session.waitList } returns waitList
        every { session.bookingList } returns bookingList
        every { session.isSessionFree() } returns true
        every { session.hostedBy } returns "admin-1"

        every { cacheManager.getRunSession("sess-7") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-4"
        every { booking.userId } returns "u-5"
        every { booking.partySize } returns 1
        every { booking.runSessionId } returns "sess-7"

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-5", "sess-7", any())
        } returns booking

        every { bookingDbService.tryLock("b-4", any()) } returns 1

        val user = mockk<User>()
        every { user.id } returns "u-5"
        every { user.stripeId } returns "cus_123"
        every { cacheManager.getUser("u-5") } returns user

        val result = service.promoteNextWaitlistedUser("sess-7")

        assertThat(result.ok).isTrue()
        assertThat(result.message).isEqualTo("User promoted (free session).")

        // extra sanity checks
        assertThat(waitList).isEmpty()
        assertThat(bookingList).hasSize(1)

        verify(exactly = 1) { sessionService.updateRunSession(session) }
        verify(exactly = 1) { bookingRepo.save(booking) }
        verify(exactly = 0) {
            adjuster.createPrimaryHoldWithChange(any(), any(), any(), any(), any(), any(), any())
        }
    }

    // ---------------------------------------------------------
    // 8) Paid session - no paymentMethod
    // ---------------------------------------------------------
    @Test
    fun `paid session with no payment method`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-6"

        val session = mockk<RunSession>()
        every { session.id } returns "sess-8"
        every { session.waitList } returns mutableListOf(runUser)
        every { session.isSessionFree() } returns false

        every { cacheManager.getRunSession("sess-8") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-5"
        every { booking.userId } returns "u-6"
        every { booking.paymentMethodId } returns null

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-6", "sess-8", any())
        } returns booking

        every { bookingDbService.tryLock("b-5", any()) } returns 1

        val user = mockk<User>()
        every { user.id } returns "u-6"
        every { user.stripeId } returns "cus_456"
        every { cacheManager.getUser("u-6") } returns user

        val result = service.promoteNextWaitlistedUser("sess-8")

        assertThat(result.ok).isFalse()
        assertThat(result.message).isEqualTo("No saved payment method for waitlist candidate.")

        verify(exactly = 1) { bookingRepo.save(booking) }
        verify(exactly = 0) { adjuster.createPrimaryHoldWithChange(any(), any(), any(), any(), any(), any(), any()) }
    }

    // ---------------------------------------------------------
    // 9) Paid session - setup requires SCA action
    // ---------------------------------------------------------
    @Test
    fun `paid session setup requires action`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-7"

        val session = mockk<RunSession>()
        every { session.id } returns "sess-9"
        every { session.waitList } returns mutableListOf(runUser)
        every { session.isSessionFree() } returns false
        every { cacheManager.getRunSession("sess-9") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-6"
        every { booking.userId } returns "u-7"
        every { booking.paymentMethodId } returns "pm_ABC"

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-7", "sess-9", any())
        } returns booking

        every { bookingDbService.tryLock("b-6", any()) } returns 1

        val user = mockk<User>()
        every { user.id } returns "u-7"
        every { user.stripeId } returns "cus_789"
        every { cacheManager.getUser("u-7") } returns user

        val setup = mockk<WaitlistSetupState>()
        every { setup.status } returns SetupStatus.REQUIRES_ACTION
        every { setup.setupIntentId } returns "si_22"

        every {
            waitlistSetupRepo.findByBookingIdAndPaymentMethodId("u-7", "pm_ABC")
        } returns setup

        val result = service.promoteNextWaitlistedUser("sess-9")

        assertThat(result.ok).isFalse()
        assertThat(result.requiresAction).isTrue()

        verify(exactly = 1) { paymentPushNotificationService.notifyPaymentActionRequired("u-7", "si_22") }
        verify(exactly = 1) { bookingRepo.save(booking) }
        verify(exactly = 0) { adjuster.createPrimaryHoldWithChange(any(), any(), any(), any(), any(), any(), any()) }
    }

    // ---------------------------------------------------------
    // 10) Paid session - successful hold creation
    // ---------------------------------------------------------
    @Test
    fun `paid session hold created successfully`() {
        val runUser = mockk<RunUser>()
        every { runUser.userId } returns "u-8"

        // Real lists
        val waitList = mutableListOf(runUser)
        val bookingList = mutableListOf<RunSession.SessionRunBooking>()

        val session = mockk<RunSession>(relaxed = true)
        every { session.id } returns "sess-10"
        every { session.waitList } returns waitList
        every { session.bookingList } returns bookingList
        every { session.isSessionFree() } returns false
        every { session.hostedBy } returns "admin-1"

        every { cacheManager.getRunSession("sess-10") } returns session

        val booking = mockk<Booking>(relaxed = true)
        every { booking.id } returns "b-7"
        every { booking.userId } returns "u-8"
        every { booking.paymentMethodId } returns "pm_456"
        every { booking.currentTotalCents } returns 2000L
        every { booking.partySize } returns 2
        every { booking.runSessionId } returns "sess-10"

        every {
            bookingRepo.findByUserIdAndRunSessionIdAndStatusIn("u-8", "sess-10", any())
        } returns booking

        every { bookingDbService.tryLock("b-7", any()) } returns 1

        val user = mockk<User>()
        every { user.id } returns "u-8"
        every { user.stripeId } returns "cus_999"
        every { cacheManager.getUser("u-8") } returns user

        val setup = mockk<WaitlistSetupState>()
        every { setup.status } returns SetupStatus.SUCCEEDED
        every {
            waitlistSetupRepo.findByBookingIdAndPaymentMethodId("u-8", "pm_456")
        } returns setup

        every {
            adjuster.createPrimaryHoldWithChange(
                "b-7",
                "u-8",
                "cus_999",
                "usd",
                2000L,
                "pm_456",
                "SYSTEM_WAITLIST_PROMOTION"
            )
        } returns PrimaryHoldResult(ok = true, paymentIntentId = "pi_123")

        val result = service.promoteNextWaitlistedUser("sess-10")

        assertThat(result.ok).isTrue()
        assertThat(result.message).contains("authorized")
        assertThat(result.paymentIntentId).isEqualTo("pi_123")

        // extra sanity checks
        assertThat(waitList).isEmpty()
        assertThat(bookingList).hasSize(1)

        verify(exactly = 1) { sessionService.updateRunSession(session) }
        verify(exactly = 1) { bookingRepo.save(booking) }
        verify(exactly = 1) {
            adjuster.createPrimaryHoldWithChange(
                "b-7",
                "u-8",
                "cus_999",
                "usd",
                2000L,
                "pm_456",
                "SYSTEM_WAITLIST_PROMOTION"
            )
        }
    }

}
