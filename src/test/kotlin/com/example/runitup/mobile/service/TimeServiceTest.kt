package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.RunSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.*

class TimeServiceTest {

    private lateinit var service: TimeService

    @BeforeEach
    fun setUp() {
        service = TimeService()
    }

    // ----------------------------------------------------
    // shouldSessionStart
    // ----------------------------------------------------

    @Test
    fun `shouldSessionStart returns true when session is in the past`() {
        val zone = ZoneId.of("America/Chicago")
        val nowZdt = ZonedDateTime.now(zone)
        val past = nowZdt.minusMinutes(5)

        val startDate = past.toLocalDate()
        val startTime = past.toLocalTime().withSecond(0).withNano(0)

        val result = service.shouldSessionStart(startDate, startTime, zone)

        assertTrue(result, "Session that started 5 minutes ago should be considered started")
    }

    @Test
    fun `shouldSessionStart returns false when session is in the future`() {
        val zone = ZoneId.of("America/Chicago")
        val nowZdt = ZonedDateTime.now(zone)
        val future = nowZdt.plusMinutes(10)

        val startDate = future.toLocalDate()
        val startTime = future.toLocalTime().withSecond(0).withNano(0)

        val result = service.shouldSessionStart(startDate, startTime, zone)

        assertFalse(result, "Session that starts 10 minutes from now should NOT be considered started")
    }

    // ----------------------------------------------------
    // hasEndedAtLeast
    // ----------------------------------------------------

    @Test
    fun `hasEndedAtLeast returns true when session ended more than minMinutes ago`() {
        val zone = ZoneId.of("America/Chicago")
        val nowZdt = ZonedDateTime.now(zone)
        val ended = nowZdt.minusMinutes(20)

        val endDate = ended.toLocalDate()
        val endTime = ended.toLocalTime().withSecond(0).withNano(0)

        val result = service.hasEndedAtLeast(
            endDate = endDate,
            endTime = endTime,
            zoneId = zone,
            minMinutes = 10
        )

        assertTrue(
            result,
            "Session that ended 20 minutes ago should be considered ended by at least 10 minutes"
        )
    }

    @Test
    fun `hasEndedAtLeast returns false when session ended less than minMinutes ago`() {
        val zone = ZoneId.of("America/Chicago")
        val nowZdt = ZonedDateTime.now(zone)
        val ended = nowZdt.minusMinutes(5)

        val endDate = ended.toLocalDate()
        val endTime = ended.toLocalTime().withSecond(0).withNano(0)

        val result = service.hasEndedAtLeast(
            endDate = endDate,
            endTime = endTime,
            zoneId = zone,
            minMinutes = 10
        )

        assertFalse(
            result,
            "Session that ended 5 minutes ago should NOT be considered ended by at least 10 minutes"
        )
    }

    // ----------------------------------------------------
    // isWithinNextDays
    // ----------------------------------------------------

    @Test
    fun `isWithinNextDays returns true when session is within the next N days`() {
        val zoneString = "America/Chicago"
        val zone = ZoneId.of(zoneString)
        val nowZdt = ZonedDateTime.now(zone)

        val daysAhead = 3L
        val sessionDate = nowZdt.plusDays(daysAhead).toLocalDate()
        val sessionTime = nowZdt.toLocalTime().withSecond(0).withNano(0)

        val session: RunSession = mock()
        whenever(session.zoneId).thenReturn(zoneString)
        whenever(session.date).thenReturn(sessionDate)
        whenever(session.startTime).thenReturn(sessionTime)

        val result = service.isWithinNextDays(session, days = 5)

        assertTrue(
            result,
            "Session scheduled 3 days from now (within 5 days) should return true"
        )
    }

    @Test
    fun `isWithinNextDays returns false when session is after the next N days`() {
        val zoneString = "America/Chicago"
        val zone = ZoneId.of(zoneString)
        val nowZdt = ZonedDateTime.now(zone)

        val daysAhead = 7L
        val sessionDate = nowZdt.plusDays(daysAhead).toLocalDate()
        val sessionTime = nowZdt.toLocalTime().withSecond(0).withNano(0)

        val session: RunSession = mock()
        whenever(session.zoneId).thenReturn(zoneString)
        whenever(session.date).thenReturn(sessionDate)
        whenever(session.startTime).thenReturn(sessionTime)

        val result = service.isWithinNextDays(session, days = 5)

        assertFalse(
            result,
            "Session scheduled 7 days from now (beyond 5 days) should return false"
        )
    }

    @Test
    fun `isWithinNextDays returns false when session is in the past`() {
        val zoneString = "America/Chicago"
        val zone = ZoneId.of(zoneString)
        val nowZdt = ZonedDateTime.now(zone)

        val sessionDate = nowZdt.minusDays(1).toLocalDate()
        val sessionTime = nowZdt.toLocalTime().withSecond(0).withNano(0)

        val session: RunSession = mock()
        whenever(session.zoneId).thenReturn(zoneString)
        whenever(session.date).thenReturn(sessionDate)
        whenever(session.startTime).thenReturn(sessionTime)

        val result = service.isWithinNextDays(session, days = 5)

        assertFalse(
            result,
            "Session that started yesterday should not be considered within the next 5 days"
        )
    }

    // ----------------------------------------------------
    // minutesBetween
    // ----------------------------------------------------

    @Test
    fun `minutesBetween returns positive duration for same-day times`() {
        val start = LocalTime.of(10, 0)
        val end = LocalTime.of(11, 30)

        val result = service.minutesBetween(start, end)

        assertEquals(90L, result, "10 00 to 11 30 should be 90 minutes")
    }

    @Test
    fun `minutesBetween returns zero for identical times`() {
        val t = LocalTime.of(14, 15)

        val result = service.minutesBetween(t, t)

        assertEquals(0L, result, "Same start and end time should result in 0 minutes")
    }

    @Test
    fun `minutesBetween returns negative when end is before start`() {
        val start = LocalTime.of(12, 0)
        val end = LocalTime.of(11, 0)

        val result = service.minutesBetween(start, end)

        assertTrue(
            result < 0,
            "When end is before start, Duration should be negative (here result = $result)"
        )
    }
}
