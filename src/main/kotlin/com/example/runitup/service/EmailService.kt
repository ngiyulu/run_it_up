package com.example.runitup.service

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
}