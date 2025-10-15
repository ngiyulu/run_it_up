package  com.example.runitup.push
import com.eatthepath.pushy.apns.ApnsClient
import com.eatthepath.pushy.apns.ApnsClientBuilder
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification
import com.eatthepath.pushy.apns.util.TokenUtil
import com.eatthepath.pushy.apns.auth.ApnsSigningKey
import com.example.runitup.web.rest.v1.dto.PushNotification
import com.example.runitup.web.rest.v1.dto.PushResult
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
//(Direct Apple Push Notification Service – APNs) ios only
@Component("apnsPushGateway")
class ApnsPushGateway(
    @Value("\${push.apns.keyPath}") private val keyResource: Resource,
    @Value("\${push.apns.keyId}") private val keyId: String,
    @Value("\${push.apns.teamId}") private val teamId: String,
    @Value("\${push.apns.bundleId}") private val bundleId: String,
    @Value("\${push.apns.useSandbox:true}") private val sandbox: Boolean
) : PushGateway {

    private val client: ApnsClient by lazy {
        val key = ApnsSigningKey.loadFromInputStream(keyResource.inputStream, teamId, keyId)
        val builder = ApnsClientBuilder().setSigningKey(key)
        if (sandbox) builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
        else builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
        builder.build()
    }

    override fun sendToTokens(tokens: List<String>, notif: PushNotification): PushResult {
        if (tokens.isEmpty()) return PushResult(0,0,0)
        val results = tokens.map { token ->
            val payload = buildJsonPayload(notif) // below
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
                val reason = resp.rejectionReason.get() ?: "unknown"
                errors.add(reason)
                if (reason.contains("BadDeviceToken", true) || reason.contains("Unregistered", true)) {
                    invalid.add(tokens[idx])
                }
            }
        }
        return PushResult(tokens.size, success, tokens.size - success, invalid, errors)
    }

    private fun buildJsonPayload(n: PushNotification): String {
        // Minimal APS; extend as needed
        val aps = mutableMapOf<String, Any>("alert" to mapOf("title" to n.title, "body" to n.body))
        n.sound?.let { aps["sound"] = it }
        n.badge?.let { aps["badge"] = it }

        val root = mutableMapOf<String, Any>("aps" to aps)
        if (n.imageUrl != null) root["image"] = n.imageUrl
        if (n.clickAction != null) root["click_action"] = n.clickAction
        if (n.data.isNotEmpty()) root.putAll(n.data)

        return jacksonObjectMapper().writeValueAsString(root)
    }
}