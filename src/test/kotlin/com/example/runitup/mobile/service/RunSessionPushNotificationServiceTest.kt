package com.example.runitup.mobile.service

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.SessionId
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.enum.PushTemplateId
import com.example.runitup.mobile.enum.PushTrigger
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals

class RunSessionPushNotificationServiceTest {

    private val phoneService: PhoneService = mock()
    private val pushService: PushService = mock()

    private lateinit var service: RunSessionPushNotificationService

    @BeforeEach
    fun setUp() {
        service = RunSessionPushNotificationService(phoneService, pushService)

        // Default: pushService returns a dummy result; we don't care about contents here
        whenever(
            pushService.sendToPhonesAudited(
                any(),
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull()
            )
        ).thenReturn(
            com.example.runitup.mobile.rest.v1.dto.PushResult(
                requested = 1,
                success = 1,
                failed = 0,
                invalidTokens = emptyList(),
                errors = emptyList()
            )
        )
    }

    // --- Helpers -------------------------------------------------------------

    private fun phone(userId: String, token: String = "token-$userId"): Phone =
        Phone(
            os = "iOS",
            model = "iPhone",
            token = token,
            phoneId = "phone-$userId",
            userId = userId,
            type = PhoneType.IOS
        )

    private fun runSessionBasic(
        id: String = "session-1",
        title: String = "Pickup Run",
        version: Int? = 3,
        city: String? = "Dallas",
        bookingUserIds: List<String> = emptyList()
    ): RunSession {
        // We mock RunSession to avoid its big constructor.
        val rs: RunSession = mock()

        whenever(rs.id).thenReturn(id)
        whenever(rs.title).thenReturn(title)
        whenever(rs.version).thenReturn(version)

        val gym: Gym? = if (city != null) {
            mock<Gym>().apply {
                whenever(this.city).thenReturn(city)
            }
        } else null
        whenever(rs.gym).thenReturn(gym)

        val bookingList = bookingUserIds.map { userId ->
            RunSession.SessionRunBooking(
                bookingId = "b-$userId",
                userId = userId,
                partySize = 1
            )
        }.toMutableList()
        whenever(rs.bookingList).thenReturn(bookingList)

        return rs
    }

    private fun user(
        id: String,
        firstName: String = "John",
        lastName: String = "Doe"
    ): User {
        val u: User = mock()
        whenever(u.id).thenReturn(id)
        whenever(u.getFullName()).thenReturn("$firstName $lastName")
        return u
    }

    private fun booking(userId: String): Booking {
        val b: Booking = mock()
        whenever(b.userId).thenReturn(userId)
        return b
    }

    // --- Tests ---------------------------------------------------------------

    @Test
    fun `notifyPlayersRunSessionConfirmed sends RUN_CONFIRMED to target user`() {
        val targetUser = "user-1"
        val session = runSessionBasic(
            id = "session-123",
            title = "Pickup Run",
            version = 5
        )

        val phones = listOf(phone(targetUser))
        whenever(phoneService.getPhonesByUser(targetUser)).thenReturn(phones)

        val phonesCaptor = argumentCaptor<List<Phone>>()
        val notifCaptor = argumentCaptor<PushNotification>()
        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()

        service.notifyPlayersRunSessionConfirmed(targetUser, session)

        // Should call getPhonesByUser, not getListOfPhone
        verify(phoneService).getPhonesByUser(targetUser)

        verify(pushService).sendToPhonesAudited(
            phonesCaptor.capture(),
            notifCaptor.capture(),
            triggerCaptor.capture(),
            eq("session-123"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        // Phones are only for the target user
        val passedUserIds = phonesCaptor.firstValue.map { it.userId }
        assertEquals(listOf(targetUser), passedUserIds)

        // Trigger & template
        assertEquals(PushTrigger.RUN_SESSION_CONFIRMED, triggerCaptor.firstValue)
        assertEquals(PushTemplateId.RUN_CONFIRMED, templateCaptor.firstValue)

        // Dedupe key: "run.confirmed:session-123:5"
        assertEquals("run.confirmed:session-123:5", dedupeCaptor.firstValue)

        // Notification payload
        val notif = notifCaptor.firstValue
        assertEquals("Session confirmation", notif.title)
        assertEquals("Session Pickup Run is confirmed", notif.body)
        assertEquals(ScreenConstant.RUN_DETAIL, notif.data[AppConstant.SCREEN])
        assertEquals("session-123", notif.data[SessionId])
    }

    @Test
    fun `runSessionCancelled filters out creator and uses RUN_CANCELLED`() {
        val creatorId = "creator-1"
        val session = runSessionBasic(
            id = "session-9",
            title = "Morning Run",
            version = 2
        )
        val bookings = listOf(
            booking("u1"),
            booking("u2"),
            booking(creatorId)
        )
        val phonesAll = listOf(
            phone("u1"),
            phone("u2"),
            phone(creatorId)
        )

        val creator = user(creatorId)

        whenever(phoneService.getListOfPhone(listOf("u1", "u2", creatorId))).thenReturn(phonesAll)

        val phonesCaptor = argumentCaptor<List<Phone>>()
        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()

        service.runSessionCancelled(session, bookings, creator)

        verify(phoneService).getListOfPhone(listOf("u1", "u2", creatorId))

        verify(pushService).sendToPhonesAudited(
            phonesCaptor.capture(),
            any(),
            triggerCaptor.capture(),
            eq("session-9"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        val passedUserIds = phonesCaptor.firstValue.map { it.userId }.sorted()
        // creator must NOT be notified
        assert(passedUserIds == listOf("u1", "u2"))

        assert(triggerCaptor.firstValue == PushTrigger.RUN_SESSION_CANCELLED)
        assert(templateCaptor.firstValue == PushTemplateId.RUN_CANCELLED)
        assert(dedupeCaptor.firstValue == "run.cancelled:session-9:2")
    }

    @Test
    fun `userJoinedRunSession notifies admin using RUN_USER_JOINED and user-scoped dedupe`() {
        val adminId = "admin-11"
        val userId = "user-99"
        val bookingId = "booking-123"
        val session = runSessionBasic(id = "sess-join")

        val adminPhones = listOf(phone(adminId))
        whenever(phoneService.getPhonesByUser(adminId)).thenReturn(adminPhones)

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()

        service.notifyAdminUserJoinedRunSession(
            adminUserId = adminId,
            user = user(userId, "Mike", "Smith"),
            runSession = session,
            bookingId = bookingId
        )

        verify(phoneService).getPhonesByUser(adminId)

        verify(pushService).sendToPhonesAudited(
            any(),
            any(),
            triggerCaptor.capture(),
            eq("sess-join"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        assert(triggerCaptor.firstValue == PushTrigger.RUN_SESSION_USER_JOINED)
        assert(templateCaptor.firstValue == PushTemplateId.RUN_USER_JOINED)
        // "run.user_joined:sess-join:user-99:booking-123"
        assert(dedupeCaptor.firstValue == "run.user_joined:sess-join:user-99:booking-123")
    }

    @Test
    fun `userJoinedWaitListRunSession notifies admin with RUN_USER_JOINED_WAITLIST template`() {
        val adminId = "admin-22"
        val userId = "user-55"
        val bookingId = "wait-1"
        val session = runSessionBasic(id = "sess-wl")

        whenever(phoneService.getPhonesByUser(adminId)).thenReturn(listOf(phone(adminId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()

        service.notifyAdminUserJoinedWaitListRunSession(
            adminUserId = adminId,
            user = user(userId),
            runSession = session,
            bookingId = bookingId
        )

        verify(phoneService).getPhonesByUser(adminId)

        verify(pushService).sendToPhonesAudited(
            any(),
            any(),
            triggerCaptor.capture(),
            eq("sess-wl"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        assert(triggerCaptor.firstValue == PushTrigger.RUN_SESSION_USER_JOINED_WAITLIST)
        assert(templateCaptor.firstValue == PushTemplateId.RUN_USER_JOINED_WAITLIST)
        assert(dedupeCaptor.firstValue == "run.user_joined_waitlist:sess-wl:user-55:wait-1")
    }

    @Test
    fun `notifyAdminUserPromotion uses RUN_USER_JOINED_WAITLIST_ADMIN template`() {
        val adminId = "admin-33"
        val userId = "user-77"
        val bookingId = "b-promote"
        val session = runSessionBasic(id = "sess-promote")

        whenever(phoneService.getPhonesByUser(adminId)).thenReturn(listOf(phone(adminId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()
        val phonesCaptor = argumentCaptor<List<Phone>>()

        service.notifyAdminUserPromotion(
            adminUserId = adminId,
            user = user(userId),
            runSession = session,
            bookingId = bookingId
        )

        verify(phoneService).getPhonesByUser(adminId)

        verify(pushService).sendToPhonesAudited(
            phonesCaptor.capture(),
            any(),
            triggerCaptor.capture(),
            eq("sess-promote"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        // phones should target the admin
        val passedUserIds = phonesCaptor.firstValue.map { it.userId }
        assertEquals(listOf(adminId), passedUserIds)

        // Trigger & template
        assertEquals(PushTrigger.RUN_SESSION_USER_JOINED_WAITLIST, triggerCaptor.firstValue)
        assertEquals(PushTemplateId.RUN_USER_JOINED_WAITLIST_ADMIN, templateCaptor.firstValue)

        // build expected dedupe from enum id so it matches your actual enum value
        val expectedDedupe =
            "${PushTemplateId.RUN_USER_JOINED_WAITLIST_ADMIN.id}:sess-promote:$userId:$bookingId"
        assertEquals(expectedDedupe, dedupeCaptor.firstValue)
    }


    @Test
    fun `runSessionBookingCancelledByAdmin uses RUN_BOOKING_CANCELLED template and booking-scoped dedupe`() {
        val targetUserId = "user-target"
        val bookingId = "b-1"
        val session = runSessionBasic(id = "sess-cancel")

        whenever(phoneService.getPhonesByUser(targetUserId))
            .thenReturn(listOf(phone(targetUserId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()
        val phonesCaptor = argumentCaptor<List<Phone>>()

        service.runSessionBookingCancelledByAdmin(
            targetUserId = targetUserId,
            runSession = session,
            bookingId = bookingId
        )

        verify(phoneService).getPhonesByUser(targetUserId)

        verify(pushService).sendToPhonesAudited(
            phonesCaptor.capture(),
            any(),                         // notif
            triggerCaptor.capture(),
            eq("sess-cancel"),             // triggerRefId
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        // 1) Phones: should target the user who got removed
        val passedUserIds = phonesCaptor.firstValue.map { it.userId }
        assertEquals(listOf(targetUserId), passedUserIds)

        // 2) Trigger & template
        assertEquals(PushTrigger.RUN_SESSION_BOOKING_CANCELLED, triggerCaptor.firstValue)
        assertEquals(PushTemplateId.RUN_BOOKING_CANCELLED, templateCaptor.firstValue)

        // 3) Dedupe key from enum id (no hard-coded string)
        val expectedDedupe =
            "${PushTemplateId.RUN_BOOKING_CANCELLED.id}:sess-cancel:$targetUserId:$bookingId"
        assertEquals(expectedDedupe, dedupeCaptor.firstValue)
    }


    @Test
    fun `notifyUserNewRunCreated uses RUN_CREATED template and user-scoped dedupe`() {
        val targetUserId = "user-2"
        val session = runSessionBasic(
            id = "sess-new",
            title = "Evening Run",
            city = "Plano"
        )

        whenever(phoneService.getPhonesByUser(targetUserId))
            .thenReturn(listOf(phone(targetUserId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()
        val notifCaptor = argumentCaptor<PushNotification>()

        service.notifyUserNewRunCreated(targetUserId, session)

        verify(phoneService).getPhonesByUser(targetUserId)

        verify(pushService).sendToPhonesAudited(
            any(),
            notifCaptor.capture(),
            triggerCaptor.capture(),
            eq("sess-new"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        assertEquals(PushTrigger.RUN_SESSION_CREATED, triggerCaptor.firstValue)
        assertEquals(PushTemplateId.RUN_CREATED, templateCaptor.firstValue)
        assertEquals("run.created:sess-new:user-2", dedupeCaptor.firstValue)

        val notif = notifCaptor.firstValue
        assertEquals("New run session", notif.title)
        assertEquals("Join a new session in Plano", notif.body)
    }

    @Test
    fun `runSessionBookingPromoted uses RUN_BOOKING_CANCELLED template but promotion message and booking-scoped dedupe`() {
        val targetUserId = "user-3"
        val bookingId = "b-promoted"
        val session = runSessionBasic(id = "sess-promoted", title = "Noon Run")

        whenever(phoneService.getPhonesByUser(targetUserId))
            .thenReturn(listOf(phone(targetUserId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()
        val notifCaptor = argumentCaptor<PushNotification>()

        service.runSessionBookingPromoted(targetUserId, session, bookingId)

        verify(phoneService).getPhonesByUser(targetUserId)

        verify(pushService).sendToPhonesAudited(
            any(),                          // phones
            notifCaptor.capture(),          // notif
            triggerCaptor.capture(),        // trigger
            eq("sess-promoted"),            // triggerRefId
            templateCaptor.capture(),       // templateId
            dedupeCaptor.capture()          // dedupeKey
        )

        assertEquals(PushTrigger.RUN_SESSION_BOOKING_CANCELLED, triggerCaptor.firstValue)
        assertEquals(PushTemplateId.RUN_BOOKING_CANCELLED, templateCaptor.firstValue)

        val expectedDedupe =
            "${PushTemplateId.RUN_BOOKING_CANCELLED.id}:sess-promoted:$targetUserId:$bookingId"
        assertEquals(expectedDedupe, dedupeCaptor.firstValue)

        val notif = notifCaptor.firstValue
        assertEquals("You have been added to session Noon Run", notif.body)
    }



    @Test
    fun `runSessionAboutToStart uses RUN_BOOKING_START_NOTIFICATION template`() {
        val targetUserId = "user-4"
        val bookingId = "b-start"
        val session = runSessionBasic(id = "sess-soon", title = "Run Soon")

        whenever(phoneService.getPhonesByUser(targetUserId)).thenReturn(listOf(phone(targetUserId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()
        val notifCaptor = argumentCaptor<PushNotification>()

        service.runSessionAboutToStart(targetUserId, session, bookingId)

        verify(phoneService).getPhonesByUser(targetUserId)

        verify(pushService).sendToPhonesAudited(
            any(),
            notifCaptor.capture(),
            triggerCaptor.capture(),
            eq("sess-soon"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        // You currently reuse RUN_SESSION_BOOKING_CANCELLED trigger; we just assert that.
        assert(triggerCaptor.firstValue == PushTrigger.RUN_SESSION_BOOKING_CANCELLED)
        assert(templateCaptor.firstValue == PushTemplateId.RUN_BOOKING_START_NOTIFICATION)
        assert(dedupeCaptor.firstValue == "run.booking_start_notification:sess-soon:user-4:b-start")

        val notif = notifCaptor.firstValue
        assert(notif.body == "Run session is about to start soon!!!")
    }

    @Test
    fun `runSessionBookingCancelledByUser notifies admin with RUN_BOOKING_CANCELLED_USER template`() {
        val adminId = "admin-44"
        val userId = "user-10"
        val bookingId = "b-user-cancel"
        val session = runSessionBasic(id = "sess-user-cancel", title = "Evening Run 2")

        whenever(phoneService.getPhonesByUser(adminId)).thenReturn(listOf(phone(adminId)))

        val triggerCaptor = argumentCaptor<PushTrigger>()
        val templateCaptor = argumentCaptor<PushTemplateId>()
        val dedupeCaptor = argumentCaptor<String>()
        val notifCaptor = argumentCaptor<PushNotification>()

        service.notifyAdminRunSessionBookingCancelledByUser(
            adminUserId = adminId,
            runSession = session,
            user = user(userId, "Chris", "Paul"),
            bookingId = bookingId
        )

        verify(phoneService).getPhonesByUser(adminId)

        verify(pushService).sendToPhonesAudited(
            any(),
            notifCaptor.capture(),
            triggerCaptor.capture(),
            eq("sess-user-cancel"),
            templateCaptor.capture(),
            dedupeCaptor.capture()
        )

        assert(triggerCaptor.firstValue == PushTrigger.RUN_SESSION_BOOKING_CANCELLED_BY_USER)
        assert(templateCaptor.firstValue == PushTemplateId.RUN_BOOKING_CANCELLED_USER)
        assert(dedupeCaptor.firstValue == "run.booking_cancelled_user:sess-user-cancel:admin-44:b-user-cancel")

        val notif = notifCaptor.firstValue
        assert(notif.body?.contains("has cancelled booking") == true)
    }
}
