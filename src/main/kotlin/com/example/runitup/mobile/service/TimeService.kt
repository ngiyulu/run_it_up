package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.RunSession
import org.springframework.stereotype.Service
import java.time.*

@Service
class TimeService {
    fun isWithinRange(
        startDate: LocalDate,
        startTime: LocalTime,
        zoneId: ZoneId,
        maxMinutes: Long
    ): Boolean {
        val now = Instant.now()
        val sessionInstant = ZonedDateTime.of(startDate, startTime, zoneId).toInstant()

        val diffMinutes = Duration.between(now, sessionInstant).toMinutes()
        return diffMinutes < 0
    }

    fun shouldSessionStart(
        startDate: LocalDate,
        startTime: LocalTime,
        zoneId: ZoneId,
    ): Boolean {
        val now = Instant.now()
        val sessionInstant = ZonedDateTime.of(startDate, startTime, zoneId).toInstant()

        val diffMinutes = Duration.between(now, sessionInstant).toMinutes()
        return diffMinutes < 0
    }

    fun hasEndedAtLeast(
        endDate: LocalDate,
        endTime: LocalTime,
        zoneId: ZoneId,
        minMinutes: Long = 10
    ): Boolean {
        val now = Instant.now()
        val endInstant = ZonedDateTime.of(endDate, endTime, zoneId).toInstant()

        val diffMinutes = Duration.between(endInstant, now).toMinutes()
        return diffMinutes >= minMinutes
    }

    fun isWithinNextDays(session: RunSession, days: Long = 5): Boolean {
        val zone = ZoneId.of(session.zoneId)

        // Build the session start as a ZonedDateTime in the session’s timezone
        val sessionStart: ZonedDateTime = ZonedDateTime.of(
            session.date,
            session.startTime,
            zone
        )

        val now: ZonedDateTime = ZonedDateTime.now(zone)
        val cutoff: ZonedDateTime = now.plusDays(days)

        return !sessionStart.isBefore(now) && !sessionStart.isAfter(cutoff)
    }

    fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        return Duration.between(start, end).toMinutes()
    }
}