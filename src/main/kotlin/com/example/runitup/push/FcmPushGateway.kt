package com.example.runitup.push
import com.example.runitup.constants.AppConstant.fcmPushGateway
import com.example.runitup.web.rest.v1.dto.PushNotification
import com.example.runitup.web.rest.v1.dto.PushResult
import com.google.firebase.messaging.*
import org.springframework.stereotype.Component

//Google Firebase is the provider
//You send all messages to Google’s FCM servers.
//
//FCM delivers Android messages directly to devices.
//
//For iOS, FCM acts as a proxy — it forwards your message to Apple’s APNs behind the scenes.
//
//So you don’t need to manage Apple certificates or APNs connections yourself.
@Component(fcmPushGateway)
class FcmPushGateway : PushGateway {

    // FCM allows up to 500 tokens per MulticastMessage
    private val maxBatch = 500

    override fun sendToTokens(tokens: List<String>, notif: PushNotification): PushResult {
        if (tokens.isEmpty()) return PushResult(0, 0, 0)

        var requested = 0
        var success = 0
        var failed = 0
        val invalid = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Build platform configs once
        val androidConfig = buildAndroidConfig(notif)
        val apnsConfig = buildApnsConfig(notif)
        val notification = buildNotification(notif)

        tokens.chunked(maxBatch).forEach { batch ->
            val builder = MulticastMessage.builder()
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .setNotification(notification)
                .putAllData(notif.data)
                .addAllTokens(batch)

            // Add a click_action for Android via data for safety (also supported via AndroidNotification)
            notif.clickAction?.let { builder.putData("click_action", it) }

            val message = builder.build()
            val resp = FirebaseMessaging.getInstance().sendMulticast(message)

            requested += batch.size
            success += resp.successCount
            failed += resp.failureCount

            // Track invalid tokens + errors
            resp.responses.forEachIndexed { idx, r ->
                if (!r.isSuccessful) {
                    val t = batch[idx]
                    val ex = r.exception
                    val msg = ex?.message.orEmpty()
                    errors += msg

                    // Prefer structured error codes when available
                    if (ex is FirebaseMessagingException) {
                        when (ex.messagingErrorCode) {
                            MessagingErrorCode.UNREGISTERED,
                            MessagingErrorCode.INVALID_ARGUMENT -> invalid += t
                            else -> {
                                // fall back to message contains
                                if (msg.contains("UNREGISTERED", true) || msg.contains("invalid-argument", true)) {
                                    invalid += t
                                }
                            }
                        }
                    } else {
                        if (msg.contains("UNREGISTERED", true) || msg.contains("invalid-argument", true)) {
                            invalid += t
                        }
                    }
                }
            }
        }

        return PushResult(
            requested = requested,
            success = success,
            failed = failed,
            invalidTokens = invalid.distinct(),
            errors = errors
        )
    }

    private fun buildAndroidConfig(notif: PushNotification): AndroidConfig {
        val androidNotifBuilder = AndroidNotification.builder()
        // Optional Android click action (handled by your app's intent filter)
        notif.clickAction?.let { androidNotifBuilder.setClickAction(it) }

        return AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .setNotification(androidNotifBuilder.build())
            .build()
    }

    private fun buildApnsConfig(notif: PushNotification): ApnsConfig {
        val apsBuilder = Aps.builder()
        notif.badge?.let { apsBuilder.setBadge(it) }
        notif.sound?.let { apsBuilder.setSound(it) }

        // If you use a category or thread-id on iOS, you could set it here as well.
        // Example:
        // apsBuilder.setCategory("CHAT_CATEGORY")

        return ApnsConfig.builder()
            .setAps(apsBuilder.build())
            .build()
    }

    private fun buildNotification(notif: PushNotification): Notification {
        return Notification.builder()
            .setTitle(notif.title)
            .setBody(notif.body)
            .setImage(notif.imageUrl)
            .build()
    }
}
