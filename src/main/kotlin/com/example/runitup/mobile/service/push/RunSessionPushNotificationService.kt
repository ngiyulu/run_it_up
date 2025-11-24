package com.example.runitup.mobile.service.push

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.SessionId
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.service.PhoneService
import com.example.runitup.mobile.service.PushService
import com.example.runitup.mobile.service.myLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Sends run-session–related push notifications and records audit trails for each send/attempt.
 *
 * Conventions:
 * - trigger: machine-readable event key (e.g., "RUN_SESSION_CONFIRMED")
 * - templateId: logical template identifier (e.g., "run.confirmed")
 * - dedupeKey: deterministic key to avoid accidental duplicates
 *      - session scoped: "<templateId>:<sessionId>:<sessionVersion>"
 *      - user scoped:    "<templateId>:<sessionId>:<userId>"
 */
@Service
class RunSessionPushNotificationService(
    private val phoneService: PhoneService,
    private val pushService: PushService) {

    private val logger = myLogger()
    // ----- Public API -----

    fun runSessionConfirmed(adminUserId: String, runSession: RunSession) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = phoneService.getListOfPhone(runSession.bookingList
            .map { it.userId }
            .filter {it != adminUserId }
        )

        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_CONFIRMED",
            triggerRefId = sessionId,
            templateId = "run.confirmed",
            dedupeKey = dedupeKeySessionVersion("run.confirmed", sessionId, runSession.version)
        )
    }

    fun runSessionCancelled(runSession: RunSession, booking:List<Booking>, runSessionCreator:User?, ) {
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
        if(runSessionCreator != null){
            logger.info("we have to filter out the admin oid admin userId = $runSessionCreator")
            phones = phones.filter { it.userId != runSessionCreator.id }
        }
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_CANCELLED",
            triggerRefId = sessionId,
            templateId = "run.cancelled",
            dedupeKey = dedupeKeySessionVersion("run.cancelled", sessionId, runSession.version)
        )
    }

    fun userJoinedRunSession(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} joined session",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        // Notify admins/host (whoever the UI expects via admin screen), not the user who just joined.
        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_USER_JOINED",
            triggerRefId = sessionId,
            templateId = "run.user_joined",
            dedupeKey = dedupeKeyUserScoped("run.user_joined", sessionId, user.id.orEmpty(), bookingId)
        )
    }

    fun userJoinedWaitListRunSession(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} joined waitlist",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        // Notify admins/host (whoever the UI expects via admin screen), not the user who just joined.
        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_USER_JOINED_WAITLIST",
            triggerRefId = sessionId,
            templateId = "run.user_joined_waitlist",
            dedupeKey = dedupeKeyUserScoped("run.user_joined_waitlist", sessionId, user.id.orEmpty(), bookingId)
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

        // Notify admins/host (whoever the UI expects via admin screen), not the user who just joined.
        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_USER_JOINED_WAITLIST",
            triggerRefId = sessionId,
            templateId = "run.user_joined_waitlist_admin",
            dedupeKey = dedupeKeyUserScoped("run.user_joined_waitlist_admin", sessionId, user.id.orEmpty(), bookingId)
        )
    }


    fun userUpdatedBooking(adminUserId: String, user: User, runSession: RunSession, bookingId: String) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "${user.getFullName()} updated booking",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.ADMIN_RUN_DETAIL,
                SessionId to sessionId
            )
        )

        // Notify admins/host (whoever the UI expects via admin screen), not the user who just joined.
        val phones = phoneService.getPhonesByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_USER_JOINED",
            triggerRefId = sessionId,
            templateId = "run.user_joined",
            dedupeKey = dedupeKeyUserScoped("run.user_joined", sessionId, user.id.orEmpty(), bookingId)
        )
    }

    fun runSessionBookingCancelledByAdmin(targetUserId: String, runSession: RunSession, bookingId:String) {
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
            trigger = "RUN_SESSION_BOOKING_CANCELLED",
            triggerRefId = sessionId,
            templateId = "run.booking_cancelled",
            dedupeKey = dedupeKeyBookingScoped("run.booking_cancelled", sessionId, targetUserId, bookingId)
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
        val templateId = "run.created"
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_CREATED",
            triggerRefId = sessionId,
            templateId = templateId,
            dedupeKey = dedupeKeyUserScoped(templateId, sessionId, targetUserId)
        )
    }


    fun runSessionBookingPromoted(targetUserId: String, runSession: RunSession, bookingId:String) {
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
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_BOOKING_CANCELLED",
            triggerRefId = sessionId,
            templateId = "run.booking_cancelled",
            dedupeKey = dedupeKeyBookingScoped("run.booking_promoted", sessionId, targetUserId, bookingId)
        )
    }



    fun runSessionBookingCancelledByUser(adminUserId: String, runSession: RunSession, user: User, bookingId:String) {
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
            trigger = "RUN_SESSION_BOOKING_CANCELLED_BY_USER",
            triggerRefId = sessionId,
            templateId = "run.booking_cancelled_user",
            dedupeKey = dedupeKeyUserScoped("run.booking_cancelled_user", sessionId, adminUserId, bookingId)
        )
    }

    private fun dedupeKeySessionVersion(templateId: String, sessionId: String, version: Int?): String {
        val v = version ?: 0
        return "$templateId:$sessionId:$v"
    }

    private fun dedupeKeyBookingScoped(
        templateId: String,
        sessionId: String,
        userId: String,
        bookingId: String?
    ): String {
        // fall back to user-scoped if you truly don't have a booking yet
        return if (!bookingId.isNullOrBlank())
            "$templateId:$sessionId:$userId:$bookingId"
        else
            "$templateId:$sessionId:$userId"
    }

    private fun dedupeKeyUserScoped(templateId: String, sessionId: String, userId: String, bookingId:String? = null): String =
        if(bookingId != null)  "$templateId:$sessionId:$userId:$bookingId" else  "$templateId:$sessionId:$userId"

}
