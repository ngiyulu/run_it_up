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
import java.time.Year

@Service
class SendGridService(

) : BaseService() {

    @Autowired
    lateinit var sendGrid: SendGrid

    @Value("\${email}")
    private lateinit var adminEmail: String


    // --- generic html sender (same as you had, kept concise) ---
    @Throws(IOException::class)
    fun sendEmailHtml(to: String, subject: String, html: String, plainTextFallback: String? = null) {
        val from = Email(adminEmail)
        val toEmail = Email(to)
        val personalization = Personalization().apply { addTo(toEmail) }

        val mail = Mail().apply {
            setFrom(from)
            setSubject(subject)
            addPersonalization(personalization)
            if (plainTextFallback != null) addContent(Content("text/plain", plainTextFallback))
            addContent(Content("text/html", html))
        }

        val request = Request().apply {
            method = Method.POST
            endpoint = "mail/send"
            body = mail.build()
        }

        val response: Response = sendGrid.api(request)
        if (response.statusCode !in 200..299) {
            throw IOException("SendGrid error ${response.statusCode}: ${response.body}")
        }
    }
}
