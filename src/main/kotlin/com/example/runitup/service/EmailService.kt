package com.example.runitup.service

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class EmailService : BaseService() {

    @Autowired
    lateinit var sendGrid: SendGrid

    @Value("\${email}")
    private lateinit var adminEmail: String

    /**
     * Plain-text only
     */
    @Throws(IOException::class)
    fun sendEmailText(to: String, subject: String, text: String) {
        val mail = baseMail(to, subject).apply {
            addContent(Content("text/plain", text))
        }
        send(mail)
    }

    /**
     * HTML with optional plain-text fallback (recommended).
     * If you pass null for plainTextFallback, only HTML will be sent.
     */
    @Throws(IOException::class)
    fun sendEmailHtml(
        to: String,
        subject: String,
        html: String,
        plainTextFallback: String? = null
    ) {
        val mail = baseMail(to, subject).apply {
            // Multipart/alternative: text first, then html
            if (plainTextFallback != null) {
                addContent(Content("text/plain", plainTextFallback))
            }
            addContent(Content("text/html", html))
        }
        send(mail)
    }

    /**
     * Backward-compatible method. If content looks like HTML, send as HTML; otherwise as plain text.
     */
    @Throws(IOException::class)
    fun sendEmail(to: String?, subject: String?, content: String?) {
        require(!to.isNullOrBlank()) { "to is required" }
        require(!subject.isNullOrBlank()) { "subject is required" }
        require(!content.isNullOrBlank()) { "content is required" }

        val looksLikeHtml = content.contains('<') && content.contains('>')
        if (looksLikeHtml) {
            sendEmailHtml(
                to = to,
                subject = subject,
                html = content,
                plainTextFallback = stripHtmlForFallback(content)
            )
        } else {
            sendEmailText(to, subject, content)
        }
    }

    // --- helpers ---

    private fun baseMail(to: String, subject: String): Mail {
        val from = Email(adminEmail)
        val toEmail = Email(to)

        val personalization = Personalization().apply {
            addTo(toEmail)
        }

        return Mail().apply {
            setFrom(from)
            setSubject(subject)
            addPersonalization(personalization)
            // Optionally set reply-to, categories, headers, etc.
            // setReplyTo(Email("support@yourdomain.com"))
            // addCategory("transactional")
        }
    }

    @Throws(IOException::class)
    private fun send(mail: Mail) {
        val request = Request().apply {
            method = Method.POST
            endpoint = "mail/send"
            body = mail.build()
        }
        val response: Response = sendGrid.api(request)
        println("SendGrid status: ${response.statusCode}")
        println("SendGrid body: ${response.body}")
        println("SendGrid headers: ${response.headers}")
        if (response.statusCode !in 200..299) {
            throw IOException("SendGrid error ${response.statusCode}: ${response.body}")
        }
    }

    /**
     * Super basic HTML stripper for fallback. Replace with a proper sanitizer if needed.
     */
    private fun stripHtmlForFallback(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p\\b[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
