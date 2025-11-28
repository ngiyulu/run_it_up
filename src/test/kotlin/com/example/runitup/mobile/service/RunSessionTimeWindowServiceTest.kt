package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class RunSessionTimeWindowServiceTest {

    private val runSessionService: RunSessionService = mock()
    private val bookingRepository: BookingRepository = mock()
    // runSessionRepository not used directly in this service, but declared in the file; no need to mock

    private lateinit var service: RunSessionTimeWindowService

    @BeforeEach
    fun setUp() {
        service = RunSessionTimeWindowService(
            runSessionService = runSessionService,
            bookingRepository = bookingRepository
        )
    }

    // Helper to build a minimal concrete RunSession (so we can mutate bookings safely)
    private fun createRunSession(
        id: String,
        status: RunStatus = RunStatus.CONFIRMED
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
            amount = 0.0,
            manualFee = 0.0,
            total = 0.0,
            maxPlayer = 10,
            title = "Pickup Run",
            updateFreeze = false,
            bookings = mutableListOf(),
            waitList = mutableListOf(),
            players = mutableListOf(),
            maxGuest = 2,
            isFree = true,
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

    // ---------------------------------------------------------
    // processOneHourBeforeRunSession
    // ---------------------------------------------------------

    @Test
    fun `processOneHourBeforeRunSession returns empty list when no sessions are claimed`() {
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val windowMinutes = 60L

        whenever(
            runSessionService.claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))
        ).thenReturn(null)

        val result = service.processOneHourBeforeRunSession(
            nowUtc = now,
            windowMinutes = windowMinutes,
            maxSessions = 2
        )

        assertTrue(result.isEmpty(), "No sessions claimed -> result should be empty")

        // Only called once because we early-return on first null
        verify(runSessionService, times(1))
            .claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))

        verifyNoInteractions(bookingRepository)
    }

    @Test
    fun `processOneHourBeforeRunSession processes single claimed session and attaches bookings`() {
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val windowMinutes = 60L

        val session = createRunSession(id = "session-1")
        val booking1: Booking = mock()
        val booking2: Booking = mock()

        whenever(
            runSessionService.claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))
        )
            .thenReturn(session)  // first call returns a session
            .thenReturn(null)     // second call ends loop early

        whenever(
            bookingRepository.findAllByRunSessionIdAndStatus(
                eq("session-1"),
                eq(BookingStatus.JOINED),
                isNull()
            )
        ).thenReturn(PageImpl(listOf(booking1, booking2)))

        val result = service.processOneHourBeforeRunSession(
            nowUtc = now,
            windowMinutes = windowMinutes,
            maxSessions = 2
        )

        assertEquals(1, result.size, "Should process exactly one session")
        val processed = result[0]
        assertEquals("session-1", processed.id)
        assertEquals(2, processed.bookings.size, "Session should have 2 bookings attached")
        assertTrue(processed.bookings.containsAll(listOf(booking1, booking2)))

        verify(runSessionService, times(2))
            .claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))

        verify(bookingRepository, times(1)).findAllByRunSessionIdAndStatus(
            eq("session-1"),
            eq(BookingStatus.JOINED),
            isNull()
        )
    }

    @Test
    fun `processOneHourBeforeRunSession processes up to maxSessions and attaches bookings for each`() {
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val windowMinutes = 60L

        val session1 = createRunSession(id = "session-1")
        val session2 = createRunSession(id = "session-2")

        val s1Booking1: Booking = mock()
        val s1Booking2: Booking = mock()
        val s2Booking1: Booking = mock()

        whenever(
            runSessionService.claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))
        )
            .thenReturn(session1)
            .thenReturn(session2)

        whenever(
            bookingRepository.findAllByRunSessionIdAndStatus(
                eq("session-1"),
                eq(BookingStatus.JOINED),
                isNull()
            )
        ).thenReturn(PageImpl(listOf(s1Booking1, s1Booking2)))

        whenever(
            bookingRepository.findAllByRunSessionIdAndStatus(
                eq("session-2"),
                eq(BookingStatus.JOINED),
                isNull()
            )
        ).thenReturn(PageImpl(listOf(s2Booking1)))

        val result = service.processOneHourBeforeRunSession(
            nowUtc = now,
            windowMinutes = windowMinutes,
            maxSessions = 2
        )

        assertEquals(2, result.size, "Should process exactly 2 sessions")
        val processed1 = result[0]
        val processed2 = result[1]

        assertEquals("session-1", processed1.id)
        assertEquals(2, processed1.bookings.size)
        assertTrue(processed1.bookings.containsAll(listOf(s1Booking1, s1Booking2)))

        assertEquals("session-2", processed2.id)
        assertEquals(1, processed2.bookings.size)
        assertTrue(processed2.bookings.contains(s2Booking1))

        verify(runSessionService, times(2))
            .claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))

        verify(bookingRepository, times(1)).findAllByRunSessionIdAndStatus(
            eq("session-1"),
            eq(BookingStatus.JOINED),
            isNull()
        )
        verify(bookingRepository, times(1)).findAllByRunSessionIdAndStatus(
            eq("session-2"),
            eq(BookingStatus.JOINED),
            isNull()
        )
    }

    @Test
    fun `processOneHourBeforeRunSession stops early when claim returns null before reaching maxSessions`() {
        val now = Instant.parse("2025-01-01T10:00:00Z")
        val windowMinutes = 60L

        val session1 = createRunSession(id = "session-1")

        whenever(
            runSessionService.claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))
        )
            .thenReturn(session1)
            .thenReturn(null) // early stop (maxSessions=3 but only 2 calls needed)

        whenever(
            bookingRepository.findAllByRunSessionIdAndStatus(
                eq("session-1"),
                eq(BookingStatus.JOINED),
                isNull()
            )
        ).thenReturn(PageImpl(emptyList()))

        val result = service.processOneHourBeforeRunSession(
            nowUtc = now,
            windowMinutes = windowMinutes,
            maxSessions = 3
        )

        assertEquals(1, result.size, "Should process only one session due to early stop")
        verify(runSessionService, times(2))
            .claimNextSessionForOneHourNotification(eq(now), eq(windowMinutes))
    }

    // ---------------------------------------------------------
    // getBooking
    // ---------------------------------------------------------

    @Test
    fun `getBooking returns joined bookings content from repository`() {
        val sessionId = "session-123"
        val booking1: Booking = mock()
        val booking2: Booking = mock()

        val page = PageImpl(listOf(booking1, booking2))

        whenever(
            bookingRepository.findAllByRunSessionIdAndStatus(
                eq(sessionId),
                eq(BookingStatus.JOINED),
                isNull()
            )
        ).thenReturn(page)

        val result = service.getBooking(sessionId)

        assertEquals(2, result.size)
        assertTrue(result.containsAll(listOf(booking1, booking2)))

        verify(bookingRepository).findAllByRunSessionIdAndStatus(
            eq(sessionId),
            eq(BookingStatus.JOINED),
            isNull()
        )
    }
}
