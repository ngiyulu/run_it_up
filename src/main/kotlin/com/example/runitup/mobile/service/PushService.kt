package com.example.runitup.mobile.service

import com.example.runitup.mobile.constants.ConfigConstant.apnsPushGateway
import com.example.runitup.mobile.constants.ConfigConstant.fcmPushGateway
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.push.PushGateway
import com.example.runitup.mobile.repository.PushDeliveryAttemptRepository
import com.example.runitup.mobile.repository.PushNotificationEventRepository

import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.rest.v1.dto.PushResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant

@Service
class PushService(
    @Autowired @Qualifier(fcmPushGateway) private val fcm: PushGateway,
    @Autowired @Qualifier(apnsPushGateway) private val apns: PushGateway,
    @Value("\${push.useApnsDirect}") var useApnsDirect: Boolean = true,
    @Autowired private val eventRepo: PushNotificationEventRepository,
    @Autowired private val attemptRepo: PushDeliveryAttemptRepository
) {

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /**
     * Audited push sender.
     * @param phones target devices
     * @param notif  payload
     * @param trigger logical event key (e.g. RUN_SESSION_CONFIRMED)
     * @param triggerRefId link to the originating entity (e.g. runSessionId)
     * @param templateId logical template (e.g. run.confirmed)
     * @param dedupeKey optional idempotency key
     */
    @Transactional
    fun sendToPhonesAudited(
        phones: List<Phone>,
        notif: PushNotification,
        trigger: String,
        triggerRefId: String?,
        templateId: String,
        dedupeKey: String? = null
    ): PushResult {

        val event = eventRepo.save(
            PushNotificationEvent(
                trigger = trigger,
                triggerRefId = triggerRefId,
                dedupeKey = dedupeKey,
                templateId = templateId,
                title = notif.title,
                bodyPreview = notif.body?.take(80),
                dataKeys = notif.data.mapValues { it.value.toString().take(64) },
                intendedCount = phones.size
            )
        )

        val (androidPhones, iosPhones) =
            phones.partition { it.type.name == "ANDROID" }

        var requested = 0; var success = 0; var failed = 0
        val invalid = mutableListOf<String>(); val errors = mutableListOf<String>()

        fun recordAttempts(
            vendor: PushVendor,
            phoneList: List<Phone>,
            result: PushResult
        ) {
            val now = Instant.now()
            val attemptDocs = phoneList.map { phone ->
                val tokenHash = sha256(phone.token)
                PushDeliveryAttempt(
                    eventId = event.id,
                    userId = phone.userId,
                    phoneId = phone.id,
                    tokenHash = tokenHash,
                    phoneType = phone.type.name,
                    vendor = vendor,
                    requestedAt = now,
                    completedAt = Instant.now(),
                    status = if (result.invalidTokens.contains(phone.token))
                        AttemptStatus.FAILED else AttemptStatus.SUCCESS,
                    vendorMessageId = null,
                    errorCode = when {
                        result.invalidTokens.contains(phone.token) -> "InvalidToken"
                        result.failed > 0 && result.success == 0 -> "BatchError"
                        else -> null
                    },
                    errorMessage = result.errors.firstOrNull(),
                    // ---- NEW FIELDS ----
                    templateId = templateId,
                    sessionId = triggerRefId        // usually your runSession.id
                )
            }
            attemptRepo.saveAll(attemptDocs)
        }

        // Android
        if (androidPhones.isNotEmpty()) {
            val r = fcm.sendToTokens(androidPhones.map { it.token }, notif)
            requested += r.requested; success += r.success; failed += r.failed
            invalid += r.invalidTokens; errors += r.errors
            recordAttempts(PushVendor.FCM, androidPhones, r)
        }

        // iOS
        if (iosPhones.isNotEmpty()) {
            val gateway = if (useApnsDirect) apns else fcm
            val vendor = if (useApnsDirect) PushVendor.APNS else PushVendor.FCM
            val r = gateway.sendToTokens(iosPhones.map { it.token }, notif)
            requested += r.requested; success += r.success; failed += r.failed
            invalid += r.invalidTokens; errors += r.errors
            recordAttempts(vendor, iosPhones, r)
        }

        val result = PushResult(requested, success, failed, invalid, errors)
        handleInvalidTokens(result)
        return result
    }

    private fun handleInvalidTokens(result: PushResult) {
        if (result.invalidTokens.isNotEmpty()) {
            // TODO: implement phoneRepo soft-delete or disable logic
        }
    }
}
