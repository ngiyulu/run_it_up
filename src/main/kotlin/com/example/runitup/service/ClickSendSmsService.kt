// src/main/kotlin/com/example/sms/ClickSendSmsService.kt
package com.example.runitup.service


import com.example.runitup.clicksend.*
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.kotlin.core.publisher.toMono

@Service
class ClickSendSmsService(
    private val clickSendWebClient: WebClient,
    private val props: ClickSendProperties
) : SmsService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 500, multiplier = 2.0)
    )

    override suspend fun sendSmsDetailed(toE164: String, message: String, tag: String?): SendResult {
        val payload = ClickSendSmsRequest(
            messages = listOf(
                ClickSendSmsMessage(
                    body = message, to = toE164,
                    from = props.senderId, custom_string = tag
                )
            )
        )

        // Get raw response so we can see message_id
        val raw = clickSendWebClient.post()
            .uri("/sms/send")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .awaitBody<Map<String, Any>>()  // requires spring-webflux coroutine ext

        // Pull fields safely
        val http = (raw["http_code"] as? Number)?.toInt()
        val respMsg = raw["response_msg"] as? String
        val data = raw["data"] as? Map<*, *>
        val messages = data?.get("messages") as? List<*>
        val first = messages?.firstOrNull() as? Map<*, *>
        val status = first?.get("status") as? String
        val msgId = first?.get("message_id") as? String

        log.info("ClickSend queued: id={}, status={}, msg={}", msgId, status, respMsg)
        return SendResult(
            accepted = http == 200,
            responseMsg = respMsg,
            providerStatus = status,
            messageId = msgId
        )
    }


    override suspend fun fetchStatus(messageId: String): String {
        val resp = clickSendWebClient.get()
            .uri("/sms/view/{id}", messageId)
            .retrieve()
            .awaitBody<Map<String, Any>>()

        // Example: resp["data"] -> { "status": "DELIVERED", ... }
        val data = resp["data"] as? Map<*, *>
        val status = data?.get("status") as? String ?: "UNKNOWN"
        log.info("ClickSend status id={} -> {}", messageId, status)
        return status
    }

}
