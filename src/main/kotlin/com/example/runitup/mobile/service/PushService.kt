package com.example.runitup.mobile.service

import com.example.runitup.mobile.constants.ConfigConstant.apnsPushGateway
import com.example.runitup.mobile.constants.ConfigConstant.fcmPushGateway
import com.example.runitup.mobile.enum.PushTemplateId
import com.example.runitup.mobile.enum.PushTrigger
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.push.PushGateway
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.PushDeliveryAttemptRepository
import com.example.runitup.mobile.repository.PushNotificationEventRepository
import com.example.runitup.mobile.rest.v1.dto.PushJobModel
import com.example.runitup.mobile.rest.v1.dto.PushJobType
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.rest.v1.dto.PushResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class PushService(
    @Autowired @Qualifier(fcmPushGateway) private val fcm: PushGateway,
    @Autowired @Qualifier(apnsPushGateway) private val apns: PushGateway,
    @Autowired private val eventRepo: PushNotificationEventRepository,
    @Autowired private val attemptRepo: PushDeliveryAttemptRepository,
    @Autowired private  val appScope: CoroutineScope,
    @Autowired private var queueService: LightSqsService
) {

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    @Transactional
    fun sendToPhonesAudited(
        phones: List<Phone>,
        notif: PushNotification,
        trigger: PushTrigger,
        triggerRefId: String?,
        templateId: PushTemplateId,
        dedupeKey: String? = null
    ): PushResult {

        val event = eventRepo.save(
            PushNotificationEvent(
                trigger = trigger.key,
                triggerRefId = triggerRefId,
                dedupeKey = dedupeKey,
                templateId = templateId.id,
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
                    templateId = templateId.id,
                    sessionId = triggerRefId
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
            val r = apns.sendToTokens(iosPhones.map { it.token }, notif)
            requested += r.requested; success += r.success; failed += r.failed
            invalid += r.invalidTokens; errors += r.errors
            recordAttempts(PushVendor.APNS, iosPhones, r)
        }

        val result = PushResult(requested, success, failed, invalid, errors)
        handleInvalidTokens(result)
        return result
    }

    private fun handleInvalidTokens(result: PushResult) {
        if (result.invalidTokens.isNotEmpty()) {
            println("there are invalid tokens")
            val data = JobEnvelope(
                jobId = UUID.randomUUID().toString(),
                taskType = "Delete bad phone tokens",
                payload = result.invalidTokens
            )
            appScope.launch {
                queueService.sendJob(QueueNames.BAD_TOKEN_JOB, data,  delaySeconds = 0)
            }

        }
    }
}
