package com.example.runitup.mobile.utility

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object AgeUtil {

    fun ageFrom(
        dateStr: String,
        zoneIdString:String,
    ): Int {
        val dob = try { LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE) }
        catch (_: DateTimeParseException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid birthDate. Use yyyy-MM-dd, e.g. 1993-10-01"
            )
        }

        val zoneId  = ZoneId.of(zoneIdString)
        val todayInZone = LocalDate.now(zoneId)
        return Period.between(dob, todayInZone).years
    }

}