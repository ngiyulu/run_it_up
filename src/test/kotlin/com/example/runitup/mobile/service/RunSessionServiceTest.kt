// src/test/kotlin/com/example/runitup/mobile/service/RunSessionServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.RunUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class RunSessionServiceTest {

    private val paymentService = mockk<PaymentService>(relaxed = true) // not used in these methods
    private val bookingRepository = mockk<BookingRepository>()

    private lateinit var service: RunSessionService

    @BeforeEach
    fun setUp() {
        service = RunSessionService().apply {
            this.paymentService = this@RunSessionServiceTest.paymentService
            this.bookingRepository = this@RunSessionServiceTest.bookingRepository
        }
    }

    @AfterEach
    fun tearDown() {
        // no-op
    }

    // ---- helpers ----

    private fun sessionWith(
        id: String = "s1",
        amount: Double = 0.0,
        maxPlayers: Int = 10
    ): RunSession {
        return RunSession(
            id = id,
            gym = null,
            location = null,
            date = LocalDate.now(),
            startTime = LocalTime.NOON,
            endTime = LocalTime.NOON.plusHours(1),
            zoneId = "America/Chicago",
            startAtUtc = Instant.now(),
            hostedBy = null,
            host = null,
            allowGuest = true,
            duration = 1,
            notes = "",
            privateRun = false,
            description = "desc",
            amount = amount,
            total = 0.0,
            maxPlayer = maxPlayers,
            title = "Pickup",
            bookings = mutableListOf(),
            waitList = mutableListOf(),
            players = mutableListOf(),
            maxGuest = 2,
            isFree = amount == 0.0,
            isFull = false
        )
    }

    private fun booking(
        id: String,
        userId: String,
        runSessionId: String,
        status: BookingStatus
    ): Booking {
        return Booking(
            id = id,
            partySize = 1,
            userId = userId,
            user = RunUser(userId = userId),
            runSessionId = runSessionId,
            sessionAmount = 10.0,
            total = 10.0,
            joinedAtFromWaitList = null,
            status = status,
            paymentMethodId = null,
            currentTotalCents = 1000L
        )
    }

    // ---- tests ----

    @Test
    fun `getRunSession returns null when not found`() {

        val result = service.getRunSession("missing")

        assertThat(result).isNull()
    }

    @Test
    fun `getRunSession loads bookings with WAITLISTED or JOINED and attaches to session`() {
        val sessionId = "s1"
        val session = sessionWith(id = sessionId)


        val returnedBookings = listOf(
            booking(id = "b1", userId = "u1", runSessionId = sessionId, status = BookingStatus.JOINED),
            booking(id = "b2", userId = "u2", runSessionId = sessionId, status = BookingStatus.WAITLISTED)
        )

        every {
            bookingRepository.findByRunSessionIdAndStatusIn(
                sessionId,
                mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED)
            )
        } returns returnedBookings

        val result = service.getRunSession(sessionId)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(sessionId)
        assertThat(result.bookings).hasSize(2)
        assertThat(result.bookings.map { it.id }).containsExactlyInAnyOrder("b1", "b2")

        verify(exactly = 1) {
            bookingRepository.findByRunSessionIdAndStatusIn(
                sessionId,
                mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED)
            )
        }
    }

    @Test
    fun `updateRunSession clears bookings and saves session`() {
        val session = sessionWith(id = "s2").apply {
            bookings.add(booking(id = "bX", userId = "uX", runSessionId = "s2", status = BookingStatus.JOINED))
        }

        val updated = service.updateRunSession(session)

        // bookings cleared
        assertThat(updated.bookings).isEmpty()
        // saved once
    }
}
