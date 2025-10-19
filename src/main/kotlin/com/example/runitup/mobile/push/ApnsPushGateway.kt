package com.example.runitup.mobile.push

import com.eatthepath.pushy.apns.ApnsClient
import com.eatthepath.pushy.apns.ApnsClientBuilder
import com.eatthepath.pushy.apns.auth.ApnsSigningKey
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification
import com.eatthepath.pushy.apns.util.TokenUtil
import com.example.runitup.mobile.constants.ConfigConstant.apnsPushGateway
import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.rest.v1.dto.PushResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.util.Base64

@Component(apnsPushGateway)
class ApnsPushGateway(
    // NEW: base64 content of the .p8 file (no quotes/newlines if possible)
    @Value("\${push.apns.keyBase64:}") private val keyBase64: String?,
    // Optional fallback for file path if base64 not provided
    @Value("\${push.apns.keyPath:}") private val keyResource: Resource?,
    @Value("\${push.apns.keyId}") private val keyId: String,
    @Value("\${push.apns.teamId}") private val teamId: String,
    @Value("\${push.apns.bundleId}") private val bundleId: String,
    @Value("\${push.apns.useSandbox:true}") private val sandbox: Boolean
) : PushGateway {

    private val client: ApnsClient by lazy {
        val signingKey = loadSigningKey()
        val builder = ApnsClientBuilder().setSigningKey(signingKey)
        if (sandbox) builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
        else builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
        builder.build()
    }

    private fun loadSigningKey(): ApnsSigningKey {
        // Prefer Base64 property if present
        if (!keyBase64.isNullOrBlank()) {
            // Be tolerant of accidental whitespace/newlines
            val normalized = keyBase64.trim().replace("\\s".toRegex(), "")
            val bytes = Base64.getDecoder().decode(normalized)
            return ByteArrayInputStream(bytes).use { input ->
                ApnsSigningKey.loadFromInputStream(input, teamId, keyId)
            }
        }

        // Fallback to file resource
        requireNotNull(keyResource) {
            "Either 'push.apns.keyPathBase64' or 'push.apns.keyPath' must be provided"
        }
        return keyResource.inputStream.use { input ->
            ApnsSigningKey.loadFromInputStream(input, teamId, keyId)
        }
    }

    override fun sendToTokens(tokens: List<String>, notif: PushNotification): PushResult {
        if (tokens.isEmpty()) return PushResult(0, 0, 0)

        val results = tokens.map { token ->
            val payload = buildJsonPayload(notif)
            val apnsToken = TokenUtil.sanitizeTokenString(token)
            val push = SimpleApnsPushNotification(apnsToken, bundleId, payload)
            client.sendNotification(push)
        }

        var success = 0
        val invalid = mutableListOf<String>()
        val errors = mutableListOf<String>()

        results.forEachIndexed { idx, future ->
            val resp = future.get()
            if (resp.isAccepted) success++ else {
                val reason = resp.rejectionReason.orElse("unknown")
                errors.add(reason)
                if (reason.contains("BadDeviceToken", true) || reason.contains("Unregistered", true)) {
                    invalid.add(tokens[idx])
                }
            }
        }
        return PushResult(tokens.size, success, tokens.size - success, invalid, errors)
    }

    private fun buildJsonPayload(n: PushNotification): String {
        val aps = mutableMapOf<String, Any>(
            "alert" to mapOf("title" to n.title, "body" to n.body)
        )
        n.sound?.let { aps["sound"] = it }
        n.badge?.let { aps["badge"] = it }

        val root = mutableMapOf<String, Any>("aps" to aps)
        n.imageUrl?.let { root["image"] = it }
        n.clickAction?.let { root["click_action"] = it }
        if (n.data.isNotEmpty()) root.putAll(n.data)

        return jacksonObjectMapper().writeValueAsString(root)
    }
}
