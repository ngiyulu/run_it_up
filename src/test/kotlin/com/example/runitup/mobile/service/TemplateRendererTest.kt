// src/test/kotlin/com/example/runitup/mobile/service/TemplateRendererTest.kt
package com.example.runitup.mobile.service

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.IContext

class TemplateRendererTest {

    @Test
    fun `render passes template name and variables to TemplateEngine`() {
        // Arrange
        val engine = mockk<TemplateEngine>()
        val renderer = TemplateRenderer(engine)

        val nameSlot = io.mockk.slot<String>()
        val ctxSlot = io.mockk.slot<IContext>()

        every { engine.process(capture(nameSlot), capture(ctxSlot)) } answers {
            // build a fake output that reflects a captured variable
            val who = ctxSlot.captured.getVariable("name") as? String ?: "unknown"
            "<h1>Hello $who</h1>"
        }

        // Act
        val html = renderer.render(
            templateName = "welcome",
            variables = mapOf("name" to "Chris", "loginUrl" to "https://app/login")
        )

        // Assert
        assertThat(nameSlot.captured).isEqualTo("welcome")
        assertThat(ctxSlot.captured.getVariable("name")).isEqualTo("Chris")
        assertThat(ctxSlot.captured.getVariable("loginUrl")).isEqualTo("https://app/login")
        assertThat(html).contains("Hello Chris")

        verify(exactly = 1) { engine.process(any<String>(), any<IContext>()) }
    }

    @Test
    fun `render works with empty variables`() {
        val engine = mockk<TemplateEngine>()
        val renderer = TemplateRenderer(engine)

        val nameSlot = io.mockk.slot<String>()
        val ctxSlot: CapturingSlot<IContext> = io.mockk.slot()

        every { engine.process(capture(nameSlot), capture(ctxSlot)) } returns "<div>ok</div>"

        val html = renderer.render("empty", emptyMap())

        assertThat(nameSlot.captured).isEqualTo("empty")
        // No variables set → retrieving an arbitrary key should return null
        assertThat(ctxSlot.captured.getVariable("anything")).isNull()
        assertThat(html).isEqualTo("<div>ok</div>")

        verify(exactly = 1) { engine.process(any<String>(), any<IContext>()) }
    }
}
