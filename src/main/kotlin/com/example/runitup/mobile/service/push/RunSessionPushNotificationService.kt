package com.example.runitup.mobile.service.push

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.SessionId
import com.example.runitup.mobile.constants.ScreenConstant
import com.example.runitup.mobile.model.Phone
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.service.PhoneDbService
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.service.PushService
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
class RunSessionPushNotificationService {

    @Autowired
    lateinit var phoneRepository: PhoneDbService

    @Autowired
    lateinit var pushService: PushService

    // ----- Public API -----

    fun runSessionConfirmed(actorUserId: String, runSession: RunSession) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Session confirmed",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = getAllUsersPhones(runSession, actorUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_CONFIRMED",
            triggerRefId = sessionId,
            templateId = "run.confirmed",
            dedupeKey = dedupeKeySessionVersion("run.confirmed", sessionId, runSession.version)
        )
    }

    fun runSessionCancelled(actorUserId: String, runSession: RunSession) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "Session cancelled",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = getAllUsersPhones(runSession, actorUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_CANCELLED",
            triggerRefId = sessionId,
            templateId = "run.cancelled",
            dedupeKey = dedupeKeySessionVersion("run.cancelled", sessionId, runSession.version)
        )
    }

    fun userJoinedRunSession(adminUserId: String, user: User, runSession: RunSession) {
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
        val phones = getPhoneByUser(adminUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_USER_JOINED",
            triggerRefId = sessionId,
            templateId = "run.user_joined",
            dedupeKey = dedupeKeyUserScoped("run.user_joined", sessionId, user.id.orEmpty())
        )
    }

    fun runSessionBookingCancelled(targetUserId: String, runSession: RunSession) {
        val sessionId = runSession.id.orEmpty()
        val notif = PushNotification(
            title = runSession.title,
            body = "You have been removed from the run session",
            data = mapOf(
                AppConstant.SCREEN to ScreenConstant.RUN_DETAIL,
                SessionId to sessionId
            )
        )

        val phones = getPhoneByUser(targetUserId)
        pushService.sendToPhonesAudited(
            phones = phones,
            notif = notif,
            trigger = "RUN_SESSION_BOOKING_CANCELLED",
            triggerRefId = sessionId,
            templateId = "run.booking_cancelled",
            dedupeKey = dedupeKeyUserScoped("run.booking_cancelled", sessionId, targetUserId)
        )
    }

    // ----- Audience helpers -----

    private fun getAllUsersPhones(runSession: RunSession, excludeUserId: String): List<Phone> {
        val userIds = runSession.bookings
            .map { it.userId }
            .filter { it != excludeUserId }
        return if (userIds.isEmpty()) emptyList() else phoneRepository.findAllByUserIdIn(userIds)
    }

    private fun getPhoneByUser(userId: String): List<Phone> =
        phoneRepository.findAllByUserId(userId)

    // ----- Dedupe helpers -----

    private fun dedupeKeySessionVersion(templateId: String, sessionId: String, version: Int?): String {
        val v = version ?: 0
        return "$templateId:$sessionId:$v"
    }

    private fun dedupeKeyUserScoped(templateId: String, sessionId: String, userId: String): String =
        "$templateId:$sessionId:$userId"
}
