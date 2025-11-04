// src/test/kotlin/com/example/runitup/mobile/utility/AgeUtilTest.kt
package com.example.runitup.mobile.utility

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

class AgeUtilTest {

    private val zone = ZoneId.systemDefault().id

    @Test
    fun `calculates correct age for user born N years ago`() {
        val yearsAgo = 25
        val dob = LocalDate.now(ZoneId.of(zone)).minusYears(yearsAgo.toLong())
        val dateStr = dob.toString() // yyyy-MM-dd format

        val age = AgeUtil.ageFrom(dateStr, zone)

        assertThat(age).isEqualTo(yearsAgo)
    }

    @Test
    fun `returns 0 for user born today`() {
        val today = LocalDate.now(ZoneId.of(zone)).toString()
        val age = AgeUtil.ageFrom(today, zone)
        assertThat(age).isZero()
    }

    @Test
    fun `throws ResponseStatusException for invalid date format`() {
        val badDate = "10-31-1995" // invalid format
        assertThatThrownBy { AgeUtil.ageFrom(badDate, zone) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
            .hasMessageContaining("Invalid birthDate")
    }

    @Test
    fun `handles leap year birthday correctly`() {
        val zoneId = ZoneId.of(zone)
        val today = LocalDate.now(zoneId)
        val dob = LocalDate.of(2000, 2, 29) // leap day
        val expectedAge = Period.between(dob, today).years

        val age = AgeUtil.ageFrom(dob.toString(), zone)
        assertThat(age).isEqualTo(expectedAge)
    }

    @Test
    fun `calculates age correctly for different time zones`() {
        val zoneId = "Asia/Tokyo"
        val dob = LocalDate.now(ZoneId.of(zoneId)).minusYears(30)
        val age = AgeUtil.ageFrom(dob.toString(), zoneId)
        assertThat(age).isEqualTo(30)
    }
}
