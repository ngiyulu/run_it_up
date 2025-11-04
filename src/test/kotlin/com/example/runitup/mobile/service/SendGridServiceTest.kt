// src/test/kotlin/com/example/runitup/mobile/service/SendGridServiceTest.kt
package com.example.runitup.mobile.service

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

class SendGridServiceTest {

    private val sendGrid = mockk<SendGrid>()
    private val service = SendGridService(sendGrid, "admin@runitup.com").apply {
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `sendEmailHtml sends POST mail_send with HTML only when no plainTextFallback and succeeds`() {
        // Arrange
        val reqSlot = slot<Request>()
        every { sendGrid.api(capture(reqSlot)) } answers {
            Response().apply {
                statusCode = 202
                body = "" // SendGrid's accepted
            }
        }

        // Act
        service.sendEmailHtml(
            to = "user@example.com",
            subject = "Welcome!",
            html = "<b>Hello</b>",
            plainTextFallback = null
        )

        // Assert request basics
        val req = reqSlot.captured
        assertThat(req.method).isEqualTo(Method.POST)
        assertThat(req.endpoint).isEqualTo("mail/send")
        val body = req.body
        assertThat(body).isNotBlank

        // Assert key JSON parts
        // (We don’t parse JSON to keep it simple; string contains are enough.)
        assertThat(body).contains("\"subject\":\"Welcome!\"")
        assertThat(body).contains("\"email\":\"admin@runitup.com\"")   // from
        assertThat(body).contains("\"email\":\"user@example.com\"")    // to
        assertThat(body).contains("\"type\":\"text/html\"")
        assertThat(body).contains("\"value\":\"<b>Hello</b>\"")
        // No plain text when fallback is null
        assertThat(body).doesNotContain("\"type\":\"text/plain\"")

        verify(exactly = 1) { sendGrid.api(any()) }
    }

    @Test
    fun `sendEmailHtml includes plain text when fallback provided`() {
        val reqSlot = slot<Request>()
        every { sendGrid.api(capture(reqSlot)) } answers {
            Response().apply {
                statusCode = 202
                body = ""
            }
        }

        service.sendEmailHtml(
            to = "user@example.com",
            subject = "Subject",
            html = "<i>hi</i>",
            plainTextFallback = "hi"
        )

        val body = reqSlot.captured.body
        assertThat(body).contains("\"type\":\"text/plain\"")
        assertThat(body).contains("\"value\":\"hi\"")
        assertThat(body).contains("\"type\":\"text/html\"")
        assertThat(body).contains("\"value\":\"<i>hi</i>\"")
    }

    @Test
    fun `sendEmailHtml throws IOException when SendGrid returns non-2xx provider status`() {
        // Arrange a provider error (e g 400)
        every { sendGrid.api(any()) } answers {
            Response().apply {
                statusCode = 400
                body = "{\"errors\":[{\"message\":\"bad\"}]}"
            }
        }

        // Act + Assert
        val ex = assertThrows<IOException> {
            service.sendEmailHtml("user@example.com", "Oops", "<p>hello</p>", null)
        }
        assertThat(ex.message).contains("SendGrid error 400")
        verify(exactly = 1) { sendGrid.api(any()) }
    }

    // --- helpers ---
    private fun Any.setPrivate(field: String, value: Any?) {
        val f = this.javaClass.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }
}
