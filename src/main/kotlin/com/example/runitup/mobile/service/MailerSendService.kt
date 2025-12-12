package com.example.runitup.mobile.service

import com.example.runitup.mobile.config.AppConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.IOException

@Service
class MailerSendService(
    webClientBuilder: WebClient.Builder,
    private val appConfig: AppConfig,
    @Value("\${mailersend.token}") private val mailerSendToken: String
) : BaseService() {

    private val logger = myLogger()

    private val client: WebClient = webClientBuilder
        .baseUrl("https://api.mailersend.com")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $mailerSendToken")
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * Similar signature/behavior to your SendGridService.
     */
    @Throws(IOException::class)
    fun sendEmailHtml(
        to: String,
        subject: String,
        html: String,
        plainTextFallback: String? = null
    ) {
        val payload = MailerSendEmailRequest(
            from = MailerSendPerson(email = appConfig.email, name = "runitup"),
            to = listOf(MailerSendPerson(email = to)),
            subject = subject,
            text = plainTextFallback,
            html = html
        )

        val response = client.post()
            .uri("/v1/email") // MailerSend send email endpoint :contentReference[oaicite:2]{index=2}
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchangeToMono { res ->
                if (res.statusCode().is2xxSuccessful) {
                    val messageId = res.headers().header("x-message-id").firstOrNull()
                    Mono.just(MailerSendSendResult(true, res.statusCode(), messageId, null))
                } else {
                    res.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { body -> MailerSendSendResult(false, res.statusCode(), null, body) }
                }
            }
            .block()

        if (response == null) {
            throw IOException("MailerSend: no response (network/client issue)")
        }

        if (!response.ok) {
            logger.error("MailerSend error ${response.status.value()}: ${response.errorBody}")
            throw IOException("MailerSend error ${response.status.value()}: ${response.errorBody}")
        }

        // Optional: you can log the message id for tracking
        logger.info("MailerSend sent. x-message-id=${response.messageId}")
    }

    // --- DTOs for the MailerSend /v1/email payload ---

    data class MailerSendEmailRequest(
        val from: MailerSendPerson,
        val to: List<MailerSendPerson>,
        val subject: String,
        val text: String? = null,
        val html: String? = null
        // You can extend later: cc, bcc, reply_to, template_id, personalization, attachments, etc.
    )

    data class MailerSendPerson(
        val email: String,
        val name: String? = null
    )

    data class MailerSendSendResult(
        val ok: Boolean,
        val status: HttpStatusCode,
        val messageId: String?,
        val errorBody: String?
    )
}
