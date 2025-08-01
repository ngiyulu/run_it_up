package com.example.runitup.service

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class EmailService: BaseService(){
    @Autowired
    lateinit var sendGrid: SendGrid

    @Value("\${email}")
    private lateinit var adminEmail: String
    @Throws(IOException::class)
    fun sendEmail(to: String?, subject: String?, content: String?) {
        println("admin email: $adminEmail")
        val from = Email(adminEmail)
        val toEmail = Email(to)
        val emailContent = Content("text/plain", content)
        val mail = Mail(from, subject, toEmail, emailContent)
        val request = Request()
        try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response: Response = sendGrid.api(request)
            println(response.statusCode)
            println(response.body)
            println(response.headers)
        } catch (ex: IOException) {
            throw ex
        }
    }
}