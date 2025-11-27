package com.example.runitup.mobile.service.push

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.SessionId
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.enum.PushTemplateId
import com.example.runitup.mobile.enum.PushTrigger
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.service.PhoneService
import com.example.runitup.mobile.service.PushService
import com.example.runitup.mobile.service.myLogger
import org.springframework.stereotype.Service

@Service
class RunSessionPushNotificationService(
    private val phoneService: PhoneService,
    private val pushService: PushService
) {

    private val logger = myLogger()

    fun runSessionCancelled(runSession: RunSession, booking: List<Booking>, runSessionCreator: User?) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Session cancelled",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        var phones = phoneService.getListOfPhone(booking.map { it.userId })
        if (runSessionCreator != null) {
            logger.info("we have to filter out the admin oid admin userId = $runSessionCreator")
            phones = phones.filter { it.userId != runSessionCreator.id }
        }

        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_CANCELLED,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_CANCELLED,
            dedupeKey = dedupeKeySessionVersion(PushTemplateId.RUN_CANCELLED, sessionId, runSession.version)
        )
    }



    fun runSessionBookingCancelledByAdmin(targetUserId: String, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "You have been removed from the run session",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(targetUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_BOOKING_CANCELLED,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_BOOKING_CANCELLED,
            dedupeKey = dedupeKeyBookingScoped(PushTemplateId.RUN_BOOKING_CANCELLED, sessionId, targetUserId, bookingId)
        )
    }

    fun notifyUserNewRunCreated(targetUserId: String, runSession: RunSession) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Join a new session in ${runSession.gym?.city ?: ""}",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(targetUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_CREATED,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_CREATED,
            dedupeKey = dedupeKeyUserScoped(PushTemplateId.RUN_CREATED, sessionId, targetUserId)
        )
    }

    fun runSessionBookingPromoted(targetUserId: String, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "You have been added to the run",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(targetUserId)
        val template = PushTemplateId.RUN_BOOKING_CANCELLED // keeping your current value
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_BOOKING_CANCELLED,
            triggerRefId = sessionId,
            templateId = template,
            dedupeKey = dedupeKeyBookingScoped(template, sessionId, targetUserId, bookingId)
        )
    }

    fun runSessionAboutToStart(targetUserId: String, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Run session is about to start soon!!!",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val template = PushTemplateId.RUN_BOOKING_START_NOTIFICATION
        val phones = phoneService.getPhonesByUser(targetUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_BOOKING_CANCELLED, // same as your current code
            triggerRefId = sessionId,
            templateId = template,
            dedupeKey = dedupeKeyBookingScoped(template, sessionId, targetUserId, bookingId)
        )
    }


    // ---- Admin notification ----

    fun notifyAdminRunSessionBookingCancelledByUser(adminUserId: String, runSession: RunSession, user: User, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} has cancelled booking",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_BOOKING_CANCELLED_BY_USER,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_BOOKING_CANCELLED_USER,
            dedupeKey = dedupeKeyUserScoped(PushTemplateId.RUN_BOOKING_CANCELLED_USER, sessionId, adminUserId, bookingId)
        )
    }

    fun notifyAdminUserUpdatedBooking(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} updated booking",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_USER_JOINED,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_USER_JOINED,
            dedupeKey = dedupeKeyUserScoped(PushTemplateId.RUN_USER_JOINED, sessionId, user.id.orEmpty(), bookingId)
        )
    }


    fun notifyAdminUserJoinedWaitListRunSession(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} joined waitlist",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_USER_JOINED_WAITLIST,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_USER_JOINED_WAITLIST,
            dedupeKey = dedupeKeyUserScoped(PushTemplateId.RUN_USER_JOINED_WAITLIST, sessionId, user.id.orEmpty(), bookingId)
        )
    }


    fun notifyAdminUserJoinedRunSession(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} joined session",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_USER_JOINED,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_USER_JOINED,
            dedupeKey = dedupeKeyUserScoped(PushTemplateId.RUN_USER_JOINED, sessionId, user.id.orEmpty(), bookingId)
        )
    }

    fun notifyAdminRunSessionConfirmed(adminUserId: String, runSession: RunSession) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getListOfPhone(
            runSession.bookingList
                .map { it.userId }
                .filter { it != adminUserId }
        )

        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_CONFIRMED,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_CONFIRMED,
            dedupeKey = dedupeKeySessionVersion(PushTemplateId.RUN_CONFIRMED, sessionId, runSession.version)
        )
    }

    fun notifyAdminUserPromotion(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} has been promoted from waitlist",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = PushTrigger.RUN_SESSION_USER_JOINED_WAITLIST,
            triggerRefId = sessionId,
            templateId = PushTemplateId.RUN_USER_JOINED_WAITLIST_ADMIN,
            dedupeKey = dedupeKeyUserScoped(PushTemplateId.RUN_USER_JOINED_WAITLIST_ADMIN, sessionId, user.id.orEmpty(), bookingId)
        )
    }

    // ---- Dedupe helpers using enums ----

    private fun dedupeKeySessionVersion(template: PushTemplateId, sessionId: String, version: Int?): String {
        val v = version ?: 0
        return "${template.id}:$sessionId:$v"
    }

    private fun dedupeKeyBookingScoped(
        template: PushTemplateId,
        sessionId: String,
        userId: String,
        bookingId: String?
    ): String {
        return if (!bookingId.isNullOrBlank())
            "${template.id}:$sessionId:$userId:$bookingId"
        else
            "${template.id}:$sessionId:$userId"
    }

    private fun dedupeKeyUserScoped(
        template: PushTemplateId,
        sessionId: String,
        userId: String,
        bookingId: String? = null
    ): String =
        if (bookingId != null)
            "${template.id}:$sessionId:$userId:$bookingId"
        else
            "${template.id}:$sessionId:$userId"
}
