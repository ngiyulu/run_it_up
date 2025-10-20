package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.RunSession
import org.springframework.stereotype.Service
import java.time.*

@Service
class TimeAndDateService {

    fun getTimeStamp(): Long{
        return Instant.now().epochSecond
    }

    /** Convert the event's local date/time + zone to an Instant, resolving DST overlaps to the later offset. */
    private fun eventInstant(event: com.example.runitup.mobile.model.RunSession): Instant {
        event.startAtUtc?.let { return it } // use cached value if present
        val zone = ZoneId.of(event.zoneId)
        val ldt  = LocalDateTime.of(event.date, event.startTime)
        var zdt  = ZonedDateTime.of(ldt, zone)
        zone.rules.getTransition(ldt)?.takeIf { it.isOverlap }?.let {
            zdt = zdt.withLaterOffsetAtOverlap()
        }
        return zdt.toInstant()
    }


    /** Hours from NOW to the event (positive => in the future; negative => in the past). */
    fun hoursUntil(event: com.example.runitup.mobile.model.RunSession, clock: Clock = Clock.systemUTC()): Double {
        val now = Instant.now(clock)
        val diffMillis = Duration.between(now, eventInstant(event)).toMillis()
        return diffMillis / 3_600_000.0
    }

    /** Whole hours difference (truncated toward zero). */
    fun wholeHoursUntil(runSession: com.example.runitup.mobile.model.RunSession, clock: Clock = Clock.systemUTC()): Long =
        Duration.between(Instant.now(clock), eventInstant(runSession)).toHours()

}