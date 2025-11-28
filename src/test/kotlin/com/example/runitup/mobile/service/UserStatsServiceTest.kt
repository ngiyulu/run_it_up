package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.UserStat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalTime

class UserStatsServiceTest {

    private val bookingRepository: BookingRepository = mock()
    private val runSessionRepository: RunSessionRepository = mock()
    private val timeService: TimeService = mock()

    private val service = UserStatsService(
        bookingRepository = bookingRepository,
        runSessionRepository = runSessionRepository,
        timeService = timeService
    )

    // Helper to build a minimal Gym
    private fun gymWithId(id: String?): Gym {
        return Gym(
            id = id ?: "gym-unknown",
            line1 = "123 Main St",
            line2 = "",
            location = null,
            fee = 10.0,
            phoneNumber = "1234567890",
            city = "City",
            title = "Test Gym",
            state = "ST",
            notes = "",
            description = "",
            country = "USA",
            zipCode = "00000"
        )
    }

    // Helper to build a minimal RunSession (only fields used in the service really matter)
    private fun runSessionWithGym(
        id: String,
        gymId: String?,
        start: LocalTime = LocalTime.of(10, 0),
        end: LocalTime = LocalTime.of(11, 0)
    ): RunSession {
        return RunSession(
            id = id,
            gym = if (gymId == null) null else gymWithId(gymId),
            location = null,
            date = LocalDate.of(2025, 1, 1),
            startTime = start,
            endTime = end,
            zoneId = "America/Chicago",
            startAtUtc = null,
            oneHourNotificationSent = false,
            hostedBy = "admin-1",
            host = null,
            allowGuest = true,
            duration = 1,
            notes = "",
            privateRun = false,
            description = "",
            amount = 10.0,
            manualFee = 0.0,
            total = 0.0,
            maxPlayer = 10,
            title = "Run",
            updateFreeze = false,
            bookings = mutableListOf(),
            waitList = mutableListOf(),
            players = mutableListOf(),
            maxGuest = 2,
            isFree = false,
            isFull = false,
            minimumPlayer = 10,
            bookingList = mutableListOf(),
            waitListBooking = mutableListOf(),
            status = RunStatus.PENDING,
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

    @Test
    fun `getUserStats returns zeros when user has no completed bookings`() {
        val userId = "user-1"

        whenever(
            bookingRepository.findByUserIdAndStatusIn(
                eq(userId),
                any()
            )
        ).thenReturn(emptyList())

        val stats: UserStat = service.getUserStats(userId)

        assertEquals(0, stats.games, "Games should be 0 when no bookings")
        assertEquals(0, stats.facilities, "Facilities should be 0 when no bookings")
        assertEquals(0L, stats.durationInMin, "Duration should be 0 when no bookings")

        verify(bookingRepository).findByUserIdAndStatusIn(eq(userId), any())
        verifyNoInteractions(runSessionRepository)
        verifyNoInteractions(timeService)
    }

    @Test
    fun `getUserStats counts games per booking, sums duration only for existing sessions, and dedupes facilities`() {
        val userId = "user-2"

        // Mock 3 bookings (COMPLETED)
        val booking1 = mock<Booking> {
            on { runSessionId } doReturn "session-1"
            on { status } doReturn BookingStatus.COMPLETED
        }
        val booking2 = mock<Booking> {
            on { runSessionId } doReturn "session-2"
            on { status } doReturn BookingStatus.COMPLETED
        }
        val booking3 = mock<Booking> {
            on { runSessionId } doReturn "session-3"
            on { status } doReturn BookingStatus.COMPLETED
        }

        val bookings = listOf(booking1, booking2, booking3)

        whenever(
            bookingRepository.findByUserIdAndStatusIn(
                eq(userId),
                any()
            )
        ).thenReturn(bookings)

        // session-1 -> null (e.g., deleted session)
        whenever(runSessionRepository.findByIdentifier("session-1"))
            .thenReturn(null)

        // session-2 -> gym " GYM1 " (with whitespace, uppercase)
        val session2 = runSessionWithGym(
            id = "session-2",
            gymId = "  GYM1  ",
            start = LocalTime.of(10, 0),
            end = LocalTime.of(11, 0)
        )
        // session-3 -> gym "gym1" (same logical gym, different casing)
        val session3 = runSessionWithGym(
            id = "session-3",
            gymId = "gym1",
            start = LocalTime.of(12, 0),
            end = LocalTime.of(13, 30)
        )

        whenever(runSessionRepository.findByIdentifier("session-2"))
            .thenReturn(session2)
        whenever(runSessionRepository.findByIdentifier("session-3"))
            .thenReturn(session3)

        // Duration from timeService
        whenever(timeService.minutesBetween(session2.startTime, session2.endTime))
            .thenReturn(60L)  // 1 hour
        whenever(timeService.minutesBetween(session3.startTime, session3.endTime))
            .thenReturn(90L)  // 1.5 hours

        val stats: UserStat = service.getUserStats(userId)

        // games increments for every booking (even if session is null)
        assertEquals(3, stats.games, "Games should equal number of completed bookings")

        // facilities: session-2 gym "  GYM1  " and session-3 gym "gym1" are same after trim+lowercase
        assertEquals(1, stats.facilities, "Facilities should dedupe gyms ignoring case and whitespace")

        // duration: only sessions that exist contribute
        assertEquals(150L, stats.durationInMin, "Duration should be sum of minutesBetween for existing sessions")

        verify(bookingRepository).findByUserIdAndStatusIn(eq(userId), any())
        verify(runSessionRepository).findByIdentifier("session-1")
        verify(runSessionRepository).findByIdentifier("session-2")
        verify(runSessionRepository).findByIdentifier("session-3")

        verify(timeService).minutesBetween(session2.startTime, session2.endTime)
        verify(timeService).minutesBetween(session3.startTime, session3.endTime)
    }

    @Test
    fun `countUniqueGymsVisited returns 0 when all gyms are null or blank`() {
        val s1 = runSessionWithGym(id = "s1", gymId = null)
        val s2 = runSessionWithGym(id = "s2", gymId = "   ")
        val visits = listOf(s1, s2)

        val count = service.countUniqueGymsVisited(visits)

        assertEquals(0, count, "Should be 0 when all gym ids are null or blank")
    }

    @Test
    fun `countUniqueGymsVisited dedupes gyms by trimmed lowercase id`() {
        val s1 = runSessionWithGym(id = "s1", gymId = "GYM1")
        val s2 = runSessionWithGym(id = "s2", gymId = " gym1  ")
        val s3 = runSessionWithGym(id = "s3", gymId = "GYM2")
        val s4 = runSessionWithGym(id = "s4", gymId = "gym2")
        val s5 = runSessionWithGym(id = "s5", gymId = null)

        val visits = listOf(s1, s2, s3, s4, s5)

        val count = service.countUniqueGymsVisited(visits)

        // gyms: "gym1", "gym2" => 2 unique
        assertEquals(2, count, "Should count unique gyms case-insensitively and trimming whitespace")
    }

    @Test
    fun `getUserStats queries only completed bookings`() {
        val userId = "user-3"

        // We don't really care about return value here, just the second argument
        whenever(
            bookingRepository.findByUserIdAndStatusIn(
                eq(userId),
                any()
            )
        ).thenReturn(emptyList())

        service.getUserStats(userId)

        val statusCaptor = argumentCaptor<MutableList<BookingStatus>>()

        verify(bookingRepository).findByUserIdAndStatusIn(eq(userId), statusCaptor.capture())

        val statusesPassed = statusCaptor.firstValue

        // Should be exactly [COMPLETED]
        assertEquals(1, statusesPassed.size)
        assertEquals(BookingStatus.COMPLETED, statusesPassed[0])
    }
}
