package com.example.runitup.mobile.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.IOException
import java.time.Year


@Service
class EmailService {

    @Autowired
    lateinit var templateRenderer: TemplateRenderer

    @Autowired
    lateinit var sendGridService: SendGridService


    @Throws(IOException::class)
    fun sendWelcomeEmail(to: String, name: String, loginUrl: String) {
        val html = templateRenderer.render(
            templateName = "welcome",
            variables = mapOf(
                "name" to name,
                "loginUrl" to loginUrl,
                "year" to Year.now().value
            )
        )
        sendGridService.sendEmailHtml(
            to = to,
            subject = "Welcome to RunItUp",
            html = html,
            plainTextFallback = """
                Welcome, $name!
                Sign in: $loginUrl
                © ${Year.now().value} RunItUp
            """.trimIndent()
        )
    }


    fun sendOtpEmail(to: String, otp: String) {
        val html = templateRenderer.render(
            templateName = "otp",
            variables = mapOf(
                "otp" to otp,
                "expirationMinutes" to 10,
                "appName" to "RunItUp",
                "year" to Year.now().value
            )
        )

        sendGridService.sendEmailHtml(
            to = to,
            subject = "Your RunItUp Verification Code",
            html = html,
            plainTextFallback = """
            Your verification code is $otp.
            This code expires in 10 minutes.
            © ${Year.now().value} RunItUp
        """.trimIndent()
        )
    }

}