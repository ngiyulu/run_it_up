package com.example.runitup.mobile.service

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.rest.v1.dto.PushResult
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RunSessionPushNotificationServiceTest {

    private val phoneService = mockk<PhoneService>(relaxed = true)
    private val pushService = mockk<PushService>(relaxed = true)

    private lateinit var service: RunSessionPushNotificationService

    @BeforeEach
    fun setUp() {
        service = RunSessionPushNotificationService(phoneService, pushService)
        clearMocks(phoneService, pushService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ------------------------------------------------------------------------
    // runSessionConfirmed
    // ------------------------------------------------------------------------

    @Test
    fun `runSessionConfirmed - notifies all non-admin participants and builds proper dedupe key`() {
        val adminId = "admin-1"
        val sessionId = "sess-1"

        val booking1 = mockk<RunSession.SessionRunBooking>()
        val booking2 = mockk<RunSession.SessionRunBooking>()
        val booking3 = mockk<RunSession.SessionRunBooking>()

        every { booking1.userId } returns "u1"
        every { booking2.userId } returns adminId
        every { booking3.userId } returns "u2"

        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Morning Run"
        every { runSession.version } returns 3
        every { runSession.bookingList } returns mutableListOf(booking1, booking2, booking3)

        val phones = listOf(mockk<Phone>(), mockk(), mockk())
        every {
            phoneService.getListOfPhone(match { it.contains("u1") && it.contains("u2") && !it.contains(adminId) && it.size == 2 })
        } returns phones

        val phonesSlot = slot<List<Phone>>()
        val notifSlot = slot<PushNotification>()
        val triggerSlot = slot<String>()
        val refIdSlot = slot<String>()
        val templateIdSlot = slot<String>()
        val dedupeSlot = slot<String>()

        every {
            pushService.sendToPhonesAudited(
                phones = capture(phonesSlot),
                notif = capture(notifSlot),
                trigger = capture(triggerSlot),
                triggerRefId = capture(refIdSlot),
                templateId = capture(templateIdSlot),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(3, 3, 0, emptyList(), emptyList())

        // ACT
        service.runSessionConfirmed(adminId, runSession)

        // ASSERT
        verify(exactly = 1) {
            phoneService.getListOfPhone(match { it.contains("u1") && it.contains("u2") && it.size == 2 })
        }
        verify(exactly = 1) {
            pushService.sendToPhonesAudited(any(), any(), any(), any(), any(), any())
        }

        assertThat(phonesSlot.captured).isEqualTo(phones)

        val notif = notifSlot.captured
        assertThat(notif.title).isEqualTo("Morning Run")
        assertThat(notif.body).isEqualTo("Session confirmed")
        assertThat(notif.data[AppConstant.SCREEN]).isEqualTo(ScreenConstant.RUN_DETAIL)
        assertThat(notif.data[AppConstant.SessionId]).isEqualTo(sessionId)

        assertThat(triggerSlot.captured).isEqualTo("RUN_SESSION_CONFIRMED")
        assertThat(refIdSlot.captured).isEqualTo(sessionId)
        assertThat(templateIdSlot.captured).isEqualTo("run.confirmed")
        assertThat(dedupeSlot.captured).isEqualTo("run.confirmed:$sessionId:3")
    }

    @Test
    fun `runSessionConfirmed - null version falls back to 0 in dedupe key`() {
        val adminId = "admin-2"
        val sessionId = "sess-2"

        val booking = mockk<RunSession.SessionRunBooking>()
        every { booking.userId } returns "u1"

        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Evening Run"
        every { runSession.version } returns 1
        every { runSession.bookingList } returns mutableListOf(booking)

        every { phoneService.getListOfPhone(any()) } returns emptyList()

        val dedupeSlot = slot<String>()
        every {
            pushService.sendToPhonesAudited(
                phones = any(),
                notif = any(),
                trigger = any(),
                triggerRefId = any(),
                templateId = any(),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(0, 0, 0, emptyList(), emptyList())

        service.runSessionConfirmed(adminId, runSession)

        assertThat(dedupeSlot.captured).isEqualTo("run.confirmed:$sessionId:1")
    }

    // ------------------------------------------------------------------------
    // runSessionCancelled
    // ------------------------------------------------------------------------

    @Test
    fun `runSessionCancelled - no creator filters nobody`() {
        val sessionId = "sess-c1"
        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Pickup Run"
        every { runSession.version } returns 5

        val booking1 = mockk<Booking>()
        val booking2 = mockk<Booking>()
        every { booking1.userId } returns "u1"
        every { booking2.userId } returns "u2"

        val bookings = listOf(booking1, booking2)

        val phones = listOf(mockk<Phone>(), mockk())
        every {
            phoneService.getListOfPhone(match { it == listOf("u1", "u2") })
        } returns phones

        val phonesSlot = slot<List<Phone>>()
        val notifSlot = slot<PushNotification>()
        val templateIdSlot = slot<String>()
        val triggerSlot = slot<String>()
        val dedupeSlot = slot<String>()

        every {
            pushService.sendToPhonesAudited(
                phones = capture(phonesSlot),
                notif = capture(notifSlot),
                trigger = capture(triggerSlot),
                triggerRefId = any(),
                templateId = capture(templateIdSlot),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(2, 2, 0, emptyList(), emptyList())

        service.runSessionCancelled(runSession, bookings, null)

        verify(exactly = 1) {
            phoneService.getListOfPhone(match { it == listOf("u1", "u2") })
        }

        val notif = notifSlot.captured
        assertThat(notif.title).isEqualTo("Pickup Run")
        assertThat(notif.body).isEqualTo("Session cancelled")
        assertThat(notif.data[AppConstant.SCREEN]).isEqualTo(ScreenConstant.RUN_DETAIL)
        assertThat(notif.data[AppConstant.SessionId]).isEqualTo(sessionId)

        assertThat(triggerSlot.captured).isEqualTo("RUN_SESSION_CANCELLED")
        assertThat(templateIdSlot.captured).isEqualTo("run.cancelled")
        assertThat(dedupeSlot.captured).isEqualTo("run.cancelled:$sessionId:5")
    }

    @Test
    fun `runSessionCancelled - creator is filtered out from recipients`() {
        val sessionId = "sess-c2"
        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "League Game"
        every { runSession.version } returns 1

        val creator = mockk<User>()
        every { creator.id } returns "admin-u"

        val booking1 = mockk<Booking>()
        val booking2 = mockk<Booking>()
        every { booking1.userId } returns "u1"
        every { booking2.userId } returns "admin-u"

        val bookings = listOf(booking1, booking2)

        // Phones returned from PhoneService
        val phoneU1 = mockk<Phone>()
        every { phoneU1.userId } returns "u1"
        val phoneAdmin = mockk<Phone>()
        every { phoneAdmin.userId } returns "admin-u"

        val phonesFromRepo = listOf(phoneU1, phoneAdmin)

        // Service will call getListOfPhone(listOf("u1", "admin-u"))
        every {
            phoneService.getListOfPhone(match { it == listOf("u1", "admin-u") })
        } returns phonesFromRepo

        val phonesSlot = slot<List<Phone>>()
        every {
            pushService.sendToPhonesAudited(
                phones = capture(phonesSlot),
                notif = any(),
                trigger = any(),
                triggerRefId = any(),
                templateId = any(),
                dedupeKey = any()
            )
        } returns PushResult(1, 1, 0, emptyList(), emptyList())

        // ACT
        service.runSessionCancelled(runSession, bookings, creator)

        // ASSERT: PhoneService was called with both IDs
        verify(exactly = 1) {
            phoneService.getListOfPhone(match { it == listOf("u1", "admin-u") })
        }

        // And the push is sent only to the non-admin phone
        assertThat(phonesSlot.captured).containsExactly(phoneU1)
    }


    // ------------------------------------------------------------------------
    // userJoinedRunSession
    // ------------------------------------------------------------------------

    @Test
    fun `userJoinedRunSession - notifies admin with correct dedupe key and body`() {
        val adminId = "admin-join"
        val userId = "user-1"
        val sessionId = "sess-j1"

        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Sunday Run"

        val user = mockk<User>()
        every { user.id } returns userId
        every { user.getFullName() } returns "John Doe"

        val phones = listOf(mockk<Phone>())
        every { phoneService.getPhonesByUser(adminId) } returns phones

        val notifSlot = slot<PushNotification>()
        val triggerSlot = slot<String>()
        val templateSlot = slot<String>()
        val dedupeSlot = slot<String>()
        every {
            pushService.sendToPhonesAudited(
                phones = any(),
                notif = capture(notifSlot),
                trigger = capture(triggerSlot),
                triggerRefId = any(),
                templateId = capture(templateSlot),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(1, 1, 0, emptyList(), emptyList())

        service.userJoinedRunSession(adminId, user, runSession, bookingId = "b-123")

        verify(exactly = 1) { phoneService.getPhonesByUser(adminId) }

        val notif = notifSlot.captured
        assertThat(notif.title).isEqualTo("Sunday Run")
        assertThat(notif.body).isEqualTo("John Doe joined session")
        assertThat(notif.data[AppConstant.SCREEN]).isEqualTo(ScreenConstant.ADMIN_RUN_DETAIL)
        assertThat(notif.data[AppConstant.SessionId]).isEqualTo(sessionId)

        assertThat(triggerSlot.captured).isEqualTo("RUN_SESSION_USER_JOINED")
        assertThat(templateSlot.captured).isEqualTo("run.user_joined")
        assertThat(dedupeSlot.captured).isEqualTo("run.user_joined:$sessionId:$userId")
    }

    // ------------------------------------------------------------------------
    // userUpdatedBooking
    // ------------------------------------------------------------------------

    @Test
    fun `userUpdatedBooking - notifies admin with correct body and user-scoped dedupe key`() {
        val adminId = "admin-upd"
        val userId = "user-2"
        val sessionId = "sess-u1"

        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Evening Pickup"

        val user = mockk<User>()
        every { user.id } returns userId
        every { user.getFullName() } returns "Jane Smith"

        val phones = listOf(mockk<Phone>())
        every { phoneService.getPhonesByUser(adminId) } returns phones

        val notifSlot = slot<PushNotification>()
        val triggerSlot = slot<String>()
        val templateSlot = slot<String>()
        val dedupeSlot = slot<String>()

        every {
            pushService.sendToPhonesAudited(
                phones = any(),
                notif = capture(notifSlot),
                trigger = capture(triggerSlot),
                triggerRefId = any(),
                templateId = capture(templateSlot),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(1, 1, 0, emptyList(), emptyList())

        service.userUpdatedBooking(adminId, user, runSession, bookingId = "b-777")

        verify(exactly = 1) { phoneService.getPhonesByUser(adminId) }

        val notif = notifSlot.captured
        assertThat(notif.title).isEqualTo("Evening Pickup")
        assertThat(notif.body).isEqualTo("Jane Smith updated booking")
        assertThat(notif.data[AppConstant.SCREEN]).isEqualTo(ScreenConstant.ADMIN_RUN_DETAIL)
        assertThat(notif.data[AppConstant.SessionId]).isEqualTo(sessionId)

        // Current implementation reuses RUN_SESSION_USER_JOINED + run.user_joined
        assertThat(triggerSlot.captured).isEqualTo("RUN_SESSION_USER_JOINED")
        assertThat(templateSlot.captured).isEqualTo("run.user_joined")
        assertThat(dedupeSlot.captured).isEqualTo("run.user_joined:$sessionId:$userId")
    }

    // ------------------------------------------------------------------------
    // runSessionBookingCancelledByAdmin
    // ------------------------------------------------------------------------

    @Test
    fun `runSessionBookingCancelledByAdmin - notifies target user with proper dedupe booking key`() {
        val sessionId = "sess-cba"
        val targetUserId = "user-x"
        val bookingId = "book-1"

        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Night Run"

        val phones = listOf(mockk<Phone>())
        every { phoneService.getPhonesByUser(targetUserId) } returns phones

        val notifSlot = slot<PushNotification>()
        val triggerSlot = slot<String>()
        val templateSlot = slot<String>()
        val dedupeSlot = slot<String>()

        every {
            pushService.sendToPhonesAudited(
                phones = any(),
                notif = capture(notifSlot),
                trigger = capture(triggerSlot),
                triggerRefId = any(),
                templateId = capture(templateSlot),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(1, 1, 0, emptyList(), emptyList())

        service.runSessionBookingCancelledByAdmin(targetUserId, runSession, bookingId)

        verify(exactly = 1) { phoneService.getPhonesByUser(targetUserId) }

        val notif = notifSlot.captured
        assertThat(notif.title).isEqualTo("Night Run")
        assertThat(notif.body).isEqualTo("You have been removed from the run session")
        assertThat(notif.data[AppConstant.SCREEN]).isEqualTo(ScreenConstant.RUN_DETAIL)
        assertThat(notif.data[AppConstant.SessionId]).isEqualTo(sessionId)

        assertThat(triggerSlot.captured).isEqualTo("RUN_SESSION_BOOKING_CANCELLED")
        assertThat(templateSlot.captured).isEqualTo("run.booking_cancelled")
        assertThat(dedupeSlot.captured).isEqualTo("run.booking_cancelled:$sessionId:$targetUserId:$bookingId")
    }

    // ------------------------------------------------------------------------
    // runSessionBookingCancelledByUser
    // ------------------------------------------------------------------------

    @Test
    fun `runSessionBookingCancelledByUser - notifies admin with proper message and user-scoped dedupe key`() {
        val adminId = "admin-cbu"
        val sessionId = "sess-cbu"
        val bookingId = "book-9"

        val runSession = mockk<RunSession>()
        every { runSession.id } returns sessionId
        every { runSession.title } returns "Lunch Run"

        val user = mockk<User>()
        every { user.id } returns "user-z"
        every { user.getFullName() } returns "Zack Taylor"

        val phones = listOf(mockk<Phone>())
        every { phoneService.getPhonesByUser(adminId) } returns phones

        val notifSlot = slot<PushNotification>()
        val triggerSlot = slot<String>()
        val templateSlot = slot<String>()
        val dedupeSlot = slot<String>()

        every {
            pushService.sendToPhonesAudited(
                phones = any(),
                notif = capture(notifSlot),
                trigger = capture(triggerSlot),
                triggerRefId = any(),
                templateId = capture(templateSlot),
                dedupeKey = capture(dedupeSlot)
            )
        } returns PushResult(1, 1, 0, emptyList(), emptyList())

        service.runSessionBookingCancelledByUser(adminId, runSession, user, bookingId)

        verify(exactly = 1) { phoneService.getPhonesByUser(adminId) }

        val notif = notifSlot.captured
        assertThat(notif.title).isEqualTo("Lunch Run")
        assertThat(notif.body).isEqualTo("Zack Taylor has cancelled booking")
        assertThat(notif.data[AppConstant.SCREEN]).isEqualTo(ScreenConstant.ADMIN_RUN_DETAIL)
        assertThat(notif.data[AppConstant.SessionId]).isEqualTo(sessionId)

        assertThat(triggerSlot.captured).isEqualTo("RUN_SESSION_BOOKING_CANCELLED_BY_USER")
        assertThat(templateSlot.captured).isEqualTo("run.booking_cancelled_user")
        assertThat(dedupeSlot.captured).isEqualTo("run.booking_cancelled_user:$sessionId:$adminId")
    }
}
