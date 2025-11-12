package com.example.runitup.mobile.service

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

    fun minutesBetween(start: LocalTime, end: LocalTime): Long {
        return Duration.between(start, end).toMinutes()
    }
}