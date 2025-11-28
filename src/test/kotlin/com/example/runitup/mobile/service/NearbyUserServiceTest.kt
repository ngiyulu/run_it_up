package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.GeoResult
import org.springframework.data.geo.GeoResults
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.query.NearQuery
import java.time.LocalDate
import java.time.LocalTime

// Mockito Kotlin
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.never

class NearbyUserServiceTest {

    private val mongoTemplate: MongoTemplate = mock()

    private val nearbyUserService = NearbyUserService(mongoTemplate)

    /**
     * Helper to build a minimal RunSession with a given gym location.
     * Adjust Gym constructor args if your actual class differs.
     */
    private fun createRunSessionWithGymLocation(location: GeoJsonPoint?): RunSession {
        val gym = Gym(
            id = "gym-1",
            line1 = "7850 Bishop Rd",
            line2 = "",
            location = location,
            image = null,
            fee = 10.0,
            phoneNumber = "1234567890",
            city = "Plano",
            title = "LA Fitness",
            state = "TX",
            notes = "test gym",
            description = "test gym",
            country = "USA",
            zipCode = "75024"
        )

        return RunSession(
            id = "session-1",
            gym = gym,
            location = location, // service only uses gym.location, but this keeps it consistent
            date = LocalDate.of(2025, 11, 17),
            startTime = LocalTime.of(12, 45),
            endTime = LocalTime.of(14, 45),
            zoneId = "America/Chicago",
            startAtUtc = null,
            oneHourNotificationSent = false,
            hostedBy = "admin-1",
            host = null,
            allowGuest = true,
            duration = 2,
            notes = "Pickup run",
            privateRun = false,
            description = "Test description",
            amount = 10.0,
            manualFee = 0.0,
            total = 0.0,
            maxPlayer = 10,
            title = "Test Run",
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
    fun `findUsersNearRunSession throws ApiRequestException when gym location is null`() {
        // Given
        val sessionWithoutLocation = createRunSessionWithGymLocation(null)

        // When / Then
        val ex = assertThrows(ApiRequestException::class.java) {
            nearbyUserService.findUsersNearRunSession(sessionWithoutLocation, radiusMiles = 10.0)
        }

        assertTrue(ex.message!!.contains("Gym location not found for session"))

        // And geoNear should never be called
        verify(mongoTemplate, never()).geoNear(any<NearQuery>(), eq(User::class.java))
    }

    @Test
    fun `findUsersNearRunSession returns nearby users from mongoTemplate geoNear`() {
        // Given
        val gymLocation = GeoJsonPoint(-96.8210647, 33.0812719)
        val session = createRunSessionWithGymLocation(gymLocation)
        val radiusMiles = 10.0

        val user1 = User(
            id = "user-1",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com"
        )

        val user2 = User(
            id = "user-2",
            firstName = "Jane",
            lastName = "Smith",
            email = "jane@example.com"
        )

        val geoResult1 = GeoResult(user1, Distance(1.0, Metrics.MILES))
        val geoResult2 = GeoResult(user2, Distance(2.0, Metrics.MILES))
        val geoResults = GeoResults(listOf(geoResult1, geoResult2), Distance(1.5, Metrics.MILES))

        whenever(mongoTemplate.geoNear(any<NearQuery>(), eq(User::class.java)))
            .thenReturn(geoResults)

        // When
        val result = nearbyUserService.findUsersNearRunSession(session, radiusMiles)

        // Then
        assertEquals(2, result.size)
        assertEquals(listOf(user1, user2), result)

        verify(mongoTemplate).geoNear(any<NearQuery>(), eq(User::class.java))
    }
}
