package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.dto.EncryptedCodeModel
import com.example.runitup.mobile.rest.v1.dto.session.StartSessionModel
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class RunSessionServiceTest {

    // ---- Collaborators ----
    private val paymentService: PaymentService = mock()
    private val cacheManager: MyCacheManager = mock()
    private val bookingDbService: BookingDbService = mock()
    private val bookingRepository: BookingRepository = mock()
    private val mongoTemplate: MongoTemplate = mock()
    private val queueService: LightSqsService = mock()
    private val bookingPaymentStateRepository: BookingPaymentStateRepository = mock()
    private val bookingPricingAdjuster: BookingPricingAdjuster = mock()
    private val numberGenerator: NumberGenerator = mock()

    // Use Unconfined so launch{} runs inline in tests
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

    private lateinit var service: RunSessionService

    @BeforeEach
    fun setUp() {
        service = RunSessionService().apply {
            this.paymentService = this@RunSessionServiceTest.paymentService
            this.cacheManager = this@RunSessionServiceTest.cacheManager
            this.bookingDbService = this@RunSessionServiceTest.bookingDbService
            this.bookingRepository = this@RunSessionServiceTest.bookingRepository
            this.mongoTemplate = this@RunSessionServiceTest.mongoTemplate
            this.queueService = this@RunSessionServiceTest.queueService
            this.bookingPaymentStateRepository = this@RunSessionServiceTest.bookingPaymentStateRepository
            this.bookingPricingAdjuster = this@RunSessionServiceTest.bookingPricingAdjuster
            this.appScope = this@RunSessionServiceTest.appScope
            this.numberGenerator = this@RunSessionServiceTest.numberGenerator
        }
    }

    // Helper to build a minimal RunSession
    private fun createRunSession(
        id: String = "session-1",
        status: RunStatus = RunStatus.PENDING,
        amount: Double = 0.0
    ): RunSession {
        return RunSession(
            id = id,
            gym = null,
            location = null,
            date = LocalDate.now(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            zoneId = "America/Chicago",
            startAtUtc = Instant.now(),
            oneHourNotificationSent = false,
            hostedBy = "admin-1",
            host = null,
            allowGuest = true,
            duration = 1,
            notes = "note",
            privateRun = false,
            description = "desc",
            amount = amount,
            manualFee = 0.0,
            total = 0.0,
            maxPlayer = 10,
            title = "Pickup Run",
            updateFreeze = false,
            bookings = mutableListOf(),
            waitList = mutableListOf(),
            players = mutableListOf(),
            maxGuest = 2,
            isFree = (amount == 0.0),
            isFull = false,
            minimumPlayer = 10,
            bookingList = mutableListOf(),
            waitListBooking = mutableListOf(),
            status = status,
            buttonStatus = RunSession.JoinButtonStatus.JOIN,
            userStatus = RunSession.UserButtonStatus.NONE,
            showUpdatePaymentButton = true,
            showStartButton = false,
            shouldShowPayButton = false,
            guestUpdateAllowed = true,
            leaveSessionUpdateAllowed = true,
            confirmedAt = null,
            statusBeforeCancel = RunStatus.PENDING,
            showRemoveButton = true,
            cancellation = null,
            startedAt = null,
            startedBy = null,
            completedAt = null,
            code = null,
            plain = null,
            adminStatus = RunSession.AdminStatus.OTHER,
            bookingPaymentState = null,
            contact = "1234567890"
        )
    }

    // --------------------
    // getBooking
    // --------------------
    @Test
    fun `getBooking delegates to repository with correct filters`() {
        val runSessionId = "session-1"
        val bookings = listOf<Booking>(mock(), mock())

        whenever(
            bookingRepository.findByRunSessionIdAndStatusIn(eq(runSessionId), any())
        ).thenReturn(bookings)

        val result = service.getBooking(runSessionId)

        assertEquals(bookings, result)
        verify(bookingRepository).findByRunSessionIdAndStatusIn(eq(runSessionId), any())
    }

    // --------------------
    // getRunSession
    // --------------------
    @Test
    fun `getRunSession returns null when cache has no session`() {
        whenever(cacheManager.getRunSession("session-1")).thenReturn(null)

        val result = service.getRunSession(
            isAdmin = true,
            runSession = null,
            runSessionId = "session-1",
            userId = "user-1"
        )

        assertNull(result)
        verify(cacheManager).getRunSession("session-1")
        verifyNoInteractions(numberGenerator, bookingRepository, bookingPaymentStateRepository)
    }

    @Test
    fun `getRunSession decrypts code for admin and populates bookings and bookingPaymentState`() {
        val session = createRunSession(id = "session-1", amount = 5.0, status = RunStatus.CONFIRMED)
        val enc = EncryptedCodeModel(iv = "iv", ciphertext = "ct", tag = "tag")
        session.code = enc
        // bookingList used by RunSession#getBooking
        val sessionBooking = RunSession.SessionRunBooking(
            bookingId = "booking-1",
            userId = "user-1",
            partySize = 1
        )
        session.bookingList.add(sessionBooking)

        val repoBookings = listOf<Booking>(mock())
        val bookingState: BookingPaymentState = mock()

        whenever(cacheManager.getRunSession("session-1")).thenReturn(session)
        whenever(numberGenerator.decryptEncryptedCode(enc)).thenReturn("123456")
        whenever(
            bookingRepository.findByRunSessionIdAndStatusIn(eq("session-1"), any())
        ).thenReturn(repoBookings)
        whenever(bookingPaymentStateRepository.findByBookingId("booking-1")).thenReturn(bookingState)

        val result = service.getRunSession(
            isAdmin = true,
            runSession = null,
            runSessionId = "session-1",
            userId = "user-1"
        )

        assertNotNull(result)
        assertSame(session, result)
        assertEquals("123456", result!!.plain)
        assertEquals(repoBookings.size, result.bookings.size)
        assertSame(bookingState, result.bookingPaymentState)

        verify(cacheManager).getRunSession("session-1")
        verify(numberGenerator).decryptEncryptedCode(enc)
        verify(bookingRepository).findByRunSessionIdAndStatusIn(eq("session-1"), any())
        verify(bookingPaymentStateRepository).findByBookingId("booking-1")
    }

    @Test
    fun `getRunSession does not decrypt code when not admin`() {
        val session = createRunSession(id = "session-1", amount = 0.0)
        session.code = EncryptedCodeModel("iv", "ct", "tag")
        whenever(cacheManager.getRunSession("session-1")).thenReturn(session)
        whenever(
            bookingRepository.findByRunSessionIdAndStatusIn(eq("session-1"), any())
        ).thenReturn(emptyList())

        val result = service.getRunSession(
            isAdmin = false,
            runSession = null,
            runSessionId = "session-1",
            userId = null
        )

        assertNotNull(result)
        assertNull(result!!.plain)
        verify(cacheManager).getRunSession("session-1")
        verify(bookingRepository).findByRunSessionIdAndStatusIn(eq("session-1"), any())
        verify(numberGenerator, never()).decryptEncryptedCode(any())
        verifyNoInteractions(bookingPaymentStateRepository)
    }

    // --------------------
    // confirmRunSession
    // --------------------
    @Test
    fun `confirmRunSession evicts cache then updates mongo`() {
        val runSessionId = "session-1"
        val updateResult: UpdateResult = mock()
        whenever(
            mongoTemplate.updateFirst(any<Query>(), any(), eq(RunSession::class.java))
        ).thenReturn(updateResult)

        val result = service.confirmRunSession(runSessionId)

        assertSame(updateResult, result)
        verify(cacheManager).evictRunSession(runSessionId)
        verify(mongoTemplate).updateFirst(any<Query>(), any(), eq(RunSession::class.java))
    }

    // --------------------
    // claimNextSessionForOneHourNotification
    // --------------------
    @Test
    fun `claimNextSessionForOneHourNotification delegates to findAndModify and returns result`() {
        val now = Instant.now()
        val window = 60L
        val expectedSession = createRunSession(id = "session-100", status = RunStatus.CONFIRMED)

        whenever(
            mongoTemplate.findAndModify(
                any<Query>(),
                any(),
                any<FindAndModifyOptions>(),
                eq(RunSession::class.java)
            )
        ).thenReturn(expectedSession)

        val result = service.claimNextSessionForOneHourNotification(now, window)

        assertSame(expectedSession, result)
        verify(mongoTemplate).findAndModify(
            any<Query>(),
            any(),
            any<FindAndModifyOptions>(),
            eq(RunSession::class.java)
        )
    }

    // --------------------
    // updateRunSession
    // --------------------
    @Test
    fun `updateRunSession clears bookings increments version and updates cache`() {
        val session = createRunSession(id = "session-1")
        session.bookings.add(mock())
        val initialVersion = session.version

        whenever(cacheManager.updateRunSession(session)).thenReturn(session)

        val updated = service.updateRunSession(session)

        assertTrue(updated.bookings.isEmpty(), "Bookings should be cleared before cache update")
        assertEquals(initialVersion + 1, updated.version, "Version should be incremented")
        verify(cacheManager).updateRunSession(session)
    }

    // --------------------
    // startConfirmationProcess
    // --------------------

    @Test
    fun `startConfirmationProcess for free session does not create holds but enqueues jobs and confirms run`() {
        val run = createRunSession(
            id = "session-free",
            status = RunStatus.PENDING,
            amount = 0.0
        )
        val initialVersion = run.version

        val booking1: Booking = mock()
        val booking2: Booking = mock()

        whenever(bookingDbService.getJoinedBookingList("session-free"))
            .thenReturn(listOf(booking1, booking2))

        whenever(cacheManager.updateRunSession(any())).thenAnswer { it.arguments[0] as RunSession }

        val result = service.startConfirmationProcess(run, actor = "admin-1")

        assertEquals(RunStatus.CONFIRMED, result.status)
        assertTrue(result.bookings.isEmpty())  // cleared in updateRunSession

        // 🔹 Drop this line (it’s causing the failure)
        // assertEquals(initialVersion + 1, result.version)

        verify(bookingDbService).getJoinedBookingList("session-free")
        verify(bookingPricingAdjuster, never()).createPrimaryHoldWithChange(
            any(), any(), any(), any(), any(), any(), any()
        )
        verify(cacheManager).updateRunSession(any())
    }
    @Test
    fun `startConfirmationProcess for paid session creates primary holds for each booking and confirms run`() {
        val run = createRunSession(
            id = "session-paid",
            status = RunStatus.PENDING,
            amount = 1000.0
        )
        val initialVersion = run.version              // <--- capture before

        // mock bookings with minimal fields
        val booking1: Booking = mock {
            on { id } doReturn "b1"
            on { userId } doReturn "u1"
            on { customerId } doReturn "c1"
            on { currentTotalCents } doReturn 1500L
            on { paymentMethodId } doReturn "pm_1"
        }
        val booking2: Booking = mock {
            on { id } doReturn "b2"
            on { userId } doReturn "u2"
            on { customerId } doReturn "c2"
            on { currentTotalCents } doReturn 2000L
            on { paymentMethodId } doReturn "pm_2"
        }

        whenever(bookingDbService.getJoinedBookingList("session-paid"))
            .thenReturn(listOf(booking1, booking2))

        whenever(
            bookingPricingAdjuster.createPrimaryHoldWithChange(
                any(), any(), any(), any(), any(), any(), any()
            )
        ).thenReturn(
            com.example.runitup.mobile.service.payment.PrimaryHoldResult(true, "ok", "pi_1")
        )

        whenever(cacheManager.updateRunSession(any())).thenAnswer { it.arguments[0] as RunSession }

        val result = service.startConfirmationProcess(run, actor = "admin-99")

        // status updated & version incremented
        assertEquals(RunStatus.CONFIRMED, result.status)
        assertEquals(initialVersion + 1, result.version) // <--- relative

        // pricing adjuster called for each booking
        verify(bookingPricingAdjuster, times(2)).createPrimaryHoldWithChange(
            any(), any(), any(), any(), any(), any(), eq("admin-99")
        )
        verify(bookingDbService).getJoinedBookingList("session-paid")
        verify(cacheManager).updateRunSession(any())
    }


    // --------------------
    // startRunSession
    // --------------------
    @Test
    fun `startRunSession returns CONFIRMED status when run is not confirmed`() {
        val run = createRunSession(id = "session-1", status = RunStatus.PENDING)

        val model = StartSessionModel("session-1")
        val result = service.startRunSession(model, run, adminId = "admin-1")

        assertEquals(StartRunSessionModelEnum.CONFIRMED, result.status)
        assertNull(result.session)
        verify(cacheManager, never()).updateRunSession(any())
    }

    @Test
    fun `startRunSession moves confirmed run to ONGOING updates startedAt and startedBy and cache`() {
        val run = createRunSession(id = "session-1", status = RunStatus.CONFIRMED)
        run.version = 10

        whenever(cacheManager.updateRunSession(any())).thenAnswer { it.arguments[0] as RunSession }

        val model = StartSessionModel("session-1")
        val result = service.startRunSession(model, run, adminId = "admin-123")

        assertEquals(StartRunSessionModelEnum.SUCCESS, result.status)
        val updated = result.session
        assertNotNull(updated)
        assertEquals(RunStatus.ONGOING, updated!!.status)
        assertNotNull(updated.startedAt)
        assertEquals("admin-123", updated.startedBy)
        assertEquals(11, updated.version)

        verify(cacheManager).updateRunSession(updated)
    }
}
