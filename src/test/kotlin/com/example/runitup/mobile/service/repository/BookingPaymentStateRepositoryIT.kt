// src/test/kotlin/com/example/runitup/mobile/repository/BookingPaymentStateRepositoryTest.kt
package com.example.runitup.mobile.service.repository

import com.example.runitup.mobile.model.BookingPaymentState
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Testcontainers
@DataMongoTest
class BookingPaymentStateRepositoryTest @Autowired constructor(
    private val repo: BookingPaymentStateRepository
) {

    companion object {
        @Container
        val mongo: MongoDBContainer = MongoDBContainer("mongo:7.0.5")

        @JvmStatic
        @DynamicPropertySource
        fun mongoProps(registry: DynamicPropertyRegistry) {
            // Override Spring Boot’s default localhost:27017 with Testcontainer URI
            registry.add("spring.data.mongodb.uri") { mongo.replicaSetUrl }
        }
    }

    @BeforeEach
    fun cleanDb() {
        repo.deleteAll()
    }

    private fun newState(
        bookingId: String,
        updatedAt: Instant
    ): BookingPaymentState {
        return BookingPaymentState(
            id = null,
            bookingId = bookingId,
            userId = "user-1",
            customerId = "cus_123",
            currency = "usd",
            totalAuthorizedCents = 1000,
            totalCapturedCents = 0,
            totalRefundedCents = 0,
            refundableRemainingCents = 1000,
            status = "PENDING",
            latestUpdatedAt = updatedAt
        )
    }

    @Test
    fun `findAllUpdatedBetween returns only states inside range`() {
        val now = Instant.now()

        val s1 = newState("b1", now.minusSeconds(3600))    // too old
        val s2 = newState("b2", now.minusSeconds(1800))    // inside range
        val s3 = newState("b3", now.plusSeconds(1800))     // inside range
        val s4 = newState("b4", now.plusSeconds(3600))     // too new

        repo.saveAll(listOf(s1, s2, s3, s4))

        val start = now.minusSeconds(2000)
        val end   = now.plusSeconds(2000)

        val result = repo.findAllUpdatedBetween(start, end)

        // Should only return s2 and s3
        assertThat(result.map { it.bookingId })
            .containsExactlyInAnyOrder("b2", "b3")

        // Validate timestamps are inside [start, end]
        result.forEach { state ->
            assertThat(state.latestUpdatedAt)
                .isAfterOrEqualTo(start)
                .isBeforeOrEqualTo(end)
        }
    }
}
