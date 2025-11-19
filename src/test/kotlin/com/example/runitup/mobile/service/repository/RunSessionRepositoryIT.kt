// src/test/kotlin/com/example/runitup/mobile/service/repository/RunSessionRepositoryIT.kt
package com.example.runitup.mobile.service.repository

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.RunSession.SessionRunBooking
import com.example.runitup.mobile.repository.RunSessionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Date

@Testcontainers
@DataMongoTest
@ActiveProfiles("test")
class RunSessionRepositoryIT @Autowired constructor(
    private val repo: RunSessionRepository
) {

    companion object {
        @Container
        val mongo: MongoDBContainer = MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
        )

        @JvmStatic
        @DynamicPropertySource
        fun mongoProps(registry: DynamicPropertyRegistry) {
            // This makes Spring Data use the Testcontainers Mongo instead of localhost:27017
            registry.add("spring.data.mongodb.uri") { mongo.replicaSetUrl }
        }
    }

    @BeforeEach
    fun cleanDb() {
        repo.deleteAll()
    }

    // Helper to build a valid RunSession matching your data class constructor
    private fun newRunSession(
        id: String? = null,
        location: GeoJsonPoint = GeoJsonPoint(-96.8, 33.0),
        date: LocalDate = LocalDate.now(),
        startTime: LocalTime = LocalTime.of(18, 0),
        endTime: LocalTime = LocalTime.of(20, 0),
        zoneId: String = "America/Chicago",
        startAtUtc: Instant = Instant.now(),
        hostedBy: String? = "host-1",
        allowGuest: Boolean = true,
        durationHours: Int = 2,
        notes: String = "",
        privateRun: Boolean = false,
        description: String = "Pickup run",
        amount: Double = 10.0,
        total: Double = 0.0,
        maxPlayer: Int = 10,
        title: String = "Evening Run",
        maxGuest: Int = 2,
        status: RunStatus = RunStatus.CONFIRMED,
        bookingList: MutableList<SessionRunBooking> = mutableListOf(),
    ): RunSession {
        return RunSession(
            id = id,
            gym = null,
            location = location,
            date = date,
            startTime = startTime,
            endTime = endTime,
            zoneId = zoneId,
            startAtUtc = startAtUtc,
            hostedBy = hostedBy,
            host = null,
            allowGuest = allowGuest,
            duration = durationHours,
            notes = notes,
            privateRun = privateRun,
            description = description,
            amount = amount,
            total = total,
            maxPlayer = maxPlayer,
            title = title,
            bookings = mutableListOf(),
            waitList = mutableListOf(),
            players = mutableListOf(),
            maxGuest = maxGuest,
            isFree = false,
            isFull = false,
            minimumPlayer = 10,
            bookingList = bookingList,
            waitListBooking = mutableListOf(),
            status = status,
            buttonStatus = RunSession.JoinButtonStatus.JOIN,
            userStatus = RunSession.UserButtonStatus.NONE,
            showUpdatePaymentButton = true,
            showStartButton = false,
            guestUpdateAllowed = true,
            leaveSessionUpdateAllowed = true,
            confirmedAt = null,
            statusBeforeCancel = RunStatus.PENDING,
            showRemoveButton = true,
            cancelledAt = null,
            startedAt = null,
            startedBy = null,
            completedAt = null,
            code = null,
            plain = null,
            bookingPaymentState = null
        )
    }

    @Test
    fun `findByIdentifier returns session with matching id`() {
        val session = newRunSession()
        val saved = repo.save(session)

        val found = repo.findByIdentifier(saved.id!!)

        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(saved.id)
        assertThat(found.title).isEqualTo("Evening Run")
    }

    @Test
    fun `findJoinableRunsExcludingUserNearOnLocalDay excludes sessions where user is already booked or waitlisted`() {
        val userId = "user-1"
        val otherUserId = "user-2"

        val nowUtc = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val startInclusive = Date.from(nowUtc.minus(1, ChronoUnit.HOURS))
        val endExclusive = Date.from(nowUtc.plus(3, ChronoUnit.HOURS))

        val withUser = newRunSession(
            location = GeoJsonPoint(-96.8000, 33.0000),
            startAtUtc = nowUtc.plus(30, ChronoUnit.MINUTES),
            bookingList = mutableListOf(
                SessionRunBooking(
                    bookingId = "b1",
                    userId = userId,
                    partySize = 1
                )
            ),
            status = RunStatus.CONFIRMED
        )

        val joinable = newRunSession(
            location = GeoJsonPoint(-96.8005, 33.0005),
            startAtUtc = nowUtc.plus(1, ChronoUnit.HOURS),
            bookingList = mutableListOf(
                SessionRunBooking(
                    bookingId = "b2",
                    userId = otherUserId,
                    partySize = 1
                )
            ),
            status = RunStatus.CONFIRMED
        )

        repo.saveAll(listOf(withUser, joinable))

        val page = repo.findJoinableRunsExcludingUserNearOnLocalDay(
            userId = userId,
            lat = 33.0000,
            lng = -96.8000,
            maxDistanceMeters = 500.0,
            statuses = listOf(RunStatus.CONFIRMED),
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            pageable = PageRequest.of(0, 10)
        )

        val ids = page.content.map { it.id }

        assertThat(ids).contains(joinable.id)
        assertThat(ids).doesNotContain(withUser.id)
    }
}
