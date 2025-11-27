// src/test/kotlin/com/example/runitup/mobile/repository/BookingRepositoryIT.kt
package com.example.runitup.mobile.service.repository

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.dto.RunUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Date

@Testcontainers
@DataMongoTest
class BookingRepositoryIT @Autowired constructor(
    private val bookingRepository: BookingRepository
) {

    companion object {
        @Container
        val mongo: MongoDBContainer = MongoDBContainer("mongo:7.0.5")

        @JvmStatic
        @DynamicPropertySource
        fun mongoProps(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongo.replicaSetUrl }
        }
    }

    @BeforeEach
    fun cleanDb() {
        bookingRepository.deleteAll()
    }

    /**
     * Helper to build a Booking with a specific createdAt.
     *
     * Adjusts RunUser to match your actual definition.
     */
    private fun newBooking(
        userId: String,
        runSessionId: String,
        createdAt: Date,
        status: BookingStatus = BookingStatus.JOINED,
        paymentStatus: PaymentStatus = PaymentStatus.PENDING,
        sessionAmount: Double = 10.0,
        total: Double = 10.0
    ): Booking {
        val runUser = RunUser(
            first = "Test",
            last = "User",
            level = null,
            userId = userId,
            imageUrl = null,
            checkIn = 0,
            guest = 0
            // verificationCode will use default from AppUtil
        )

        val booking = Booking(
            id = null,
            partySize = 1,
            userId = userId,
            user = runUser,
            runSessionId = runSessionId,
            paymentStatus = paymentStatus,
            sessionAmount = sessionAmount,
            total = total,
            paymentId = null,
            setupIntentId = null,
            paymentMethodId = null,
            checkInNumber = 0,
            joinedAtFromWaitList = null,
            status = status,
            cancelledAt = null,
            cancelledBy = null,
            isLocked = false,
            currentTotalCents = 0L,
            isLockedAt = null,
            promotedAt = null,
            customerId = null,
            paymentAuthorization = emptyList(),
            bookingPaymentState = null,
            completedAt = null,
            paidAt = null,
            date = ""
        )

        // Assuming BaseModel.createdAt is Instant
        booking.createdAt = createdAt.toInstant()

        // If BaseModel.createdAt is java.util.Date instead, use:
        // booking.createdAt = createdAt

        return booking
    }

    // -------------------------------------------------------------------------
    // findAllByDateBetweenByUser (custom @Query on createdAt range)
    // -------------------------------------------------------------------------

    @Test
    fun `findAllByDateBetweenByUser returns bookings between dates`() {
        val now = Date()

        val older = Date(now.time - 2 * 60 * 60 * 1000)  // now - 2h
        val start = Date(now.time - 60 * 60 * 1000)      // now - 1h
        val middle = Date(now.time - 30 * 60 * 1000)     // now - 30m
        val end = Date(now.time + 30 * 60 * 1000)        // now + 30m
        val newer = Date(now.time + 2 * 60 * 60 * 1000)  // now + 2h

        // Outside range
        val b1 = newBooking(
            userId = "user-1",
            runSessionId = "session-1",
            createdAt = older
        )
        val b5 = newBooking(
            userId = "user-1",
            runSessionId = "session-1",
            createdAt = newer
        )

        // Inside [start, end]
        val b2 = newBooking(
            userId = "user-1",
            runSessionId = "session-1",
            createdAt = start
        )
        val b3 = newBooking(
            userId = "user-2",
            runSessionId = "session-2",
            createdAt = middle
        )
        val b4 = newBooking(
            userId = "user-3",
            runSessionId = "session-3",
            createdAt = end
        )

        bookingRepository.saveAll(listOf(b1, b2, b3, b4, b5))

        val page = bookingRepository.findAllByDateBetweenByUser(
            startInclusive = start,
            endInclusive = end,
            pageable = PageRequest.of(0, 10)
        )

        val resultIds = page.content.map { it.id!! }

        assertThat(resultIds)
            .hasSize(3)
            .contains(b2.id, b3.id, b4.id)
            .doesNotContain(b1.id, b5.id)
    }

    // -------------------------------------------------------------------------
    // findByDate (custom @Query with $eq on createdAt)
    // -------------------------------------------------------------------------

    @Test
    fun `findByDate returns only bookings with exact createdAt`() {
        val exactDate = Date()
        val otherDate = Date(exactDate.time + 60 * 60 * 1000) // +1h

        val b1 = newBooking(
            userId = "user-1",
            runSessionId = "session-1",
            createdAt = exactDate
        )
        val b2 = newBooking(
            userId = "user-2",
            runSessionId = "session-2",
            createdAt = exactDate
        )
        val b3 = newBooking(
            userId = "user-3",
            runSessionId = "session-3",
            createdAt = otherDate
        )

        bookingRepository.saveAll(listOf(b1, b2, b3))

        val page = bookingRepository.findByDate(
            date = exactDate,
            pageable = PageRequest.of(0, 10)
        )

        val resultIds = page.content.map { it.id!! }

        assertThat(resultIds)
            .hasSize(2)
            .contains(b1.id, b2.id)
            .doesNotContain(b3.id)
    }
}
