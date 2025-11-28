package com.example.runitup.mobile.service

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.payment.CancelAuthorizationResult
import com.example.runitup.web.dto.Role
import com.stripe.param.PaymentIntentCancelParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono

class LeaveSessionServiceTest {

    private val runSessionService: RunSessionService = mock()
    private val paymentService: PaymentService = mock()
    private val waitListPaymentService: WaitListPaymentService = mock()
    private val bookingDbService: BookingDbService = mock()
    private val cacheManager: MyCacheManager = mock()
    private val textService: TextService = mock()
    private val messagingService: MessagingService = mock()
    private val bookingPricingAdjuster: BookingPricingAdjuster = mock()
    private val queueService: LightSqsService = mock()
    private val bookingRepository: BookingRepository = mock()

    // Use a simple real scope; we’re not asserting on coroutines here
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

    private lateinit var service: LeaveSessionService

    @BeforeEach
    fun setUp() {
        // Fix for lateinit bookingRepository: mock the getter on the mocked BookingDbService
        whenever(bookingDbService.bookingRepository).thenReturn(bookingRepository)

        service = LeaveSessionService(
            runSessionService = runSessionService,
            paymentService = paymentService,
            waitListPaymentService = waitListPaymentService,
            bookingDbService = bookingDbService,
            cacheManager = cacheManager,
            textService = textService,
            messagingService = messagingService,
            bookingPricingAdjuster = bookingPricingAdjuster,
            appScope = appScope,
            queueService = queueService
        )

        // Default: messaging removeParticipant returns empty Mono so .block() is safe
        whenever(messagingService.removeParticipant(any()))
            .thenReturn(Mono.empty())
    }

    // ------------------------------------------------------------------------
    // 1) Session not found
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking throws invalid_session_id when session not found`() {
        val user: User = mock {
            on { id } doReturn "user-1"
        }

        whenever(cacheManager.getRunSession("sess-1")).thenReturn(null)
        whenever(textService.getText(eq("invalid_session_id"), any()))
            .thenReturn("Invalid session id")

        val ex = assertThrows(ApiRequestException::class.java) {
            service.cancelBooking(user, "sess-1", null)
        }

        assertEquals("Invalid session id", ex.message)
        verify(cacheManager).getRunSession("sess-1")
        verifyNoMoreInteractions(bookingDbService, messagingService, bookingPricingAdjuster)
    }

    // ------------------------------------------------------------------------
    // 2) Session not deletable
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking throws invalid_session_cancel when session is not deletable`() {
        val user: User = mock {
            on { id } doReturn "user-2"
        }

        val run: RunSession = mock {
            on { id } doReturn "sess-2"
            on { isDeletable() } doReturn false
        }

        whenever(cacheManager.getRunSession("sess-2")).thenReturn(run)
        whenever(textService.getText(eq("invalid_session_cancel"), any()))
            .thenReturn("Not deletable")

        val ex = assertThrows(ApiRequestException::class.java) {
            service.cancelBooking(user, "sess-2", null)
        }

        assertEquals("Not deletable", ex.message)
        verify(cacheManager).getRunSession("sess-2")
        verify(run).isDeletable()
        verifyNoMoreInteractions(bookingDbService, messagingService, bookingPricingAdjuster)
    }

    // ------------------------------------------------------------------------
    // 3) Unauthorized admin
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking throws unauthorized_user when admin is not host or super admin`() {
        val user: User = mock {
            on { id } doReturn "user-3"
        }

        val run: RunSession = mock {
            on { id } doReturn "sess-3"
            on { isDeletable() } doReturn true
            on { hostedBy } doReturn "host-123"
        }

        val admin: AdminUser = mock {
            on { id } doReturn "admin-1"
            on { role } doReturn Role.ADMIN   // not SUPER_ADMIN
        }

        whenever(cacheManager.getRunSession("sess-3")).thenReturn(run)
        whenever(textService.getText(eq("unauthorized_user"), any()))
            .thenReturn("Unauthorized")

        val ex = assertThrows(ApiRequestException::class.java) {
            service.cancelBooking(user, "sess-3", admin)
        }

        assertEquals("Unauthorized", ex.message)
        verify(cacheManager).getRunSession("sess-3")
        verify(run).isDeletable()
        verify(run).hostedBy
        verifyNoMoreInteractions(bookingDbService, messagingService, bookingPricingAdjuster)
    }

    // ------------------------------------------------------------------------
    // 4) Booking not found
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking throws invalid_params when booking not found`() {
        val user: User = mock {
            on { id } doReturn "user-4"
        }

        val run: RunSession = mock {
            on { id } doReturn "sess-4"
            on { isDeletable() } doReturn true
            on { hostedBy } doReturn "user-4"
        }

        whenever(cacheManager.getRunSession("sess-4")).thenReturn(run)
        whenever(
            bookingDbService.getBooking("user-4", "sess-4")
        ).thenReturn(null)

        whenever(textService.getText(eq("invalid_params"), any()))
            .thenReturn("Invalid params")

        val ex = assertThrows(ApiRequestException::class.java) {
            service.cancelBooking(user, "sess-4", null)
        }

        assertEquals("Invalid params", ex.message)
        verify(cacheManager).getRunSession("sess-4")
        verify(bookingDbService).getBooking("user-4", "sess-4")
        verifyNoMoreInteractions(bookingDbService, messagingService, bookingPricingAdjuster)
    }

    // ------------------------------------------------------------------------
    // 5) Booking locked
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking throws try-again when booking is locked`() {
        val user: User = mock {
            on { id } doReturn "user-5"
        }

        val run: RunSession = mock {
            on { id } doReturn "sess-5"
            on { isDeletable() } doReturn true
            on { hostedBy } doReturn "user-5"
        }

        val booking: Booking = mock {
            on { id } doReturn "b-locked"
            on { isLocked } doReturn true
        }

        whenever(cacheManager.getRunSession("sess-5")).thenReturn(run)
        whenever(
            bookingDbService.getBooking("user-5", "sess-5")
        ).thenReturn(booking)

        whenever(textService.getText(eq("try-again"), any()))
            .thenReturn("try again later")

        val ex = assertThrows(ApiRequestException::class.java) {
            service.cancelBooking(user, "sess-5", null)
        }

        assertEquals("try again later", ex.message)
        verify(bookingDbService).getBooking("user-5", "sess-5")
        verify(booking).isLocked
        verifyNoMoreInteractions(bookingDbService, messagingService, bookingPricingAdjuster)
    }

    // ------------------------------------------------------------------------
    // 6) Happy path – FREE session (no payment cancel)
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking succeeds for free session without payment cancel`() {
        val userId = "user-6"
        val sessionId = "sess-free"

        val user: User = mock {
            on { id } doReturn userId
            on { stripeId } doReturn null    // not used for free session
        }

        val run: RunSession = mock {
            on { id } doReturn sessionId
            on { isDeletable() } doReturn true
            on { hostedBy } doReturn userId
            on { isSessionFree() } doReturn true
            on { status } doReturn RunStatus.CONFIRMED
            on { bookingList } doReturn mutableListOf<RunSession.SessionRunBooking>()
            on { bookings } doReturn mutableListOf<Booking>()
            on { waitList } doReturn mutableListOf<RunUser>()
        }

        val booking: Booking = mock {
            on { id } doReturn "b-free"
            on { isLocked } doReturn false
            on { status } doReturn BookingStatus.JOINED
            on { paymentId } doReturn null
            on { setupIntentId } doReturn null
        }

        whenever(cacheManager.getRunSession(sessionId)).thenReturn(run)
        whenever(bookingDbService.getBooking(userId, sessionId)).thenReturn(booking)
        whenever(bookingRepository.save(booking)).thenReturn(booking)

        val (savedBooking, savedRun) = service.cancelBooking(user, sessionId, null)

        // Ensure bookingRepo.save was called
        verify(bookingRepository).save(booking)

        // Messaging service called with correct model (NOTE: conversationId, not runSessionId)
        verify(messagingService).removeParticipant(
            check {
                assertEquals(userId, it.userId)
                assertEquals(sessionId, it.conversationId)
            }
        )

        // For free session we should NOT cancel waitlist SetupIntent
        verify(waitListPaymentService, never()).cancelWaitlistSetupIntent(any())

        assertSame(booking, savedBooking)
        assertSame(run, savedRun)
    }

    // ------------------------------------------------------------------------
    // 7) Happy path – PAID session, non-pending, payment cancel + waitlist cancel
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking cancels payment and waitlist when paid session and not pending`() {
        val userId = "user-7"
        val sessionId = "sess-paid"

        val user: User = mock {
            on { id } doReturn userId
            on { stripeId } doReturn "cus_123"
        }

        val run: RunSession = mock {
            on { id } doReturn sessionId
            on { isDeletable() } doReturn true
            on { hostedBy } doReturn userId
            on { isSessionFree() } doReturn false
            on { status } doReturn RunStatus.CONFIRMED
            on { bookingList } doReturn mutableListOf<RunSession.SessionRunBooking>()
            on { bookings } doReturn mutableListOf<Booking>()
            on { waitList } doReturn mutableListOf<RunUser>()
        }

        val booking: Booking = mock {
            on { id } doReturn "b-paid"
            on { isLocked } doReturn false
            on { status } doReturn BookingStatus.JOINED
            on { paymentId } doReturn "pi_123"
            on { setupIntentId } doReturn "si_123"
        }

        whenever(cacheManager.getRunSession(sessionId)).thenReturn(run)
        whenever(bookingDbService.getBooking(userId, sessionId)).thenReturn(booking)
        whenever(bookingRepository.save(booking)).thenReturn(booking)

        whenever(
            bookingPricingAdjuster.cancelAuthorizationAndUpdate(
                bookingId = eq("b-paid"),
                userId = eq(userId),
                customerId = eq("cus_123"),
                currency = eq("us"),
                paymentIntentId = eq("pi_123"),
                reason = eq(PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER)
            )
        ).thenReturn(
            CancelAuthorizationResult(
                ok = true,
                paymentIntentId = "pi_123",
                stripeStatus = "canceled"
            )
        )

        val (savedBooking, savedRun) = service.cancelBooking(user, sessionId, null)

        // Payment cancel invoked
        verify(bookingPricingAdjuster).cancelAuthorizationAndUpdate(
            bookingId = eq("b-paid"),
            userId = eq(userId),
            customerId = eq("cus_123"),
            currency = eq("us"),
            paymentIntentId = eq("pi_123"),
            reason = eq(PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER)
        )

        // Waitlist SetupIntent cancel called
        verify(waitListPaymentService).cancelWaitlistSetupIntent("si_123")

        // Booking persisted
        verify(bookingRepository).save(booking)

        // Participant removed from conversation
        verify(messagingService).removeParticipant(
            check {
                assertEquals(userId, it.userId)
                assertEquals(sessionId, it.conversationId)
            }
        )

        assertSame(booking, savedBooking)
        assertSame(run, savedRun)
    }

    // ------------------------------------------------------------------------
    // 8) Payment cancel fails -> throws payment_error
    // ------------------------------------------------------------------------
    @Test
    fun `cancelBooking throws payment_error when cancelAuthorizationAndUpdate fails`() {
        val userId = "user-8"
        val sessionId = "sess-paid-fail"

        val user: User = mock {
            on { id } doReturn userId
            on { stripeId } doReturn "cus_999"
        }

        val run: RunSession = mock {
            on { id } doReturn sessionId
            on { isDeletable() } doReturn true
            on { hostedBy } doReturn userId
            on { isSessionFree() } doReturn false
            on { status } doReturn RunStatus.CONFIRMED
        }

        val booking: Booking = mock {
            on { id } doReturn "b-fail"
            on { isLocked } doReturn false
            on { status } doReturn BookingStatus.JOINED
            on { paymentId } doReturn "pi_fail"
            on { setupIntentId } doReturn "si_fail"
        }

        whenever(cacheManager.getRunSession(sessionId)).thenReturn(run)
        whenever(bookingDbService.getBooking(userId, sessionId)).thenReturn(booking)

        whenever(
            bookingPricingAdjuster.cancelAuthorizationAndUpdate(
                bookingId = eq("b-fail"),
                userId = eq(userId),
                customerId = eq("cus_999"),
                currency = eq("us"),
                paymentIntentId = eq("pi_fail"),
                reason = eq(PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER)
            )
        ).thenReturn(
            CancelAuthorizationResult(
                ok = false,
                paymentIntentId = "pi_fail",
                stripeStatus = "requires_payment_method",
                message = "Card error"
            )
        )

        val ex = assertThrows(ApiRequestException::class.java) {
            service.cancelBooking(user, sessionId, null)
        }

        // Your service throws generic "payment_error" string
        assertEquals("payment_error", ex.message)

        verify(bookingPricingAdjuster).cancelAuthorizationAndUpdate(
            bookingId = eq("b-fail"),
            userId = eq(userId),
            customerId = eq("cus_999"),
            currency = eq("us"),
            paymentIntentId = eq("pi_fail"),
            reason = eq(PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER)
        )

        // Waitlist cancel should not be called when primary cancel fails
        verify(waitListPaymentService, never()).cancelWaitlistSetupIntent(any())
        verifyNoMoreInteractions(bookingRepository, messagingService)
    }
}
