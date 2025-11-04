// src/test/kotlin/com/example/runitup/mobile/service/TextServiceTest.kt
package com.example.runitup.mobile.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import java.util.*

class TextServiceTest {

    private val messageSource = io.mockk.mockk<MessageSource>()
    private val service = TextService(messageSource)
    @AfterEach fun tearDown() = clearAllMocks()

    @Test
    fun `getText delegates to MessageSource with correct key and locale`() {
        val key = "welcome.title"
        val localeTag = "en-US"

        val localeSlot = slot<Locale>()
        every { messageSource.getMessage(key, null, capture(localeSlot)) } returns "Welcome"

        val result = service.getText(key, localeTag)

        assertThat(result).isEqualTo("Welcome")
        // locale parsing should use BCP47 tag
        assertThat(localeSlot.captured.language).isEqualTo("en")
        assertThat(localeSlot.captured.country).isEqualTo("US")

        verify(exactly = 1) { messageSource.getMessage(key, null, any<Locale>()) }
    }

    @Test
    fun `getTextWithPlaceHolder passes args and locale, returns rendered text`() {
        val code = "greeting.named"
        val args = arrayOf("Chris")
        val localeTag = "fr-FR"

        val localeSlot = slot<Locale>()

        // Note: MessageSource#getMessage(String, Object[], Locale) → use Array<Any> matcher
        every {
            messageSource.getMessage(code, any<Array<Any>>(), capture(localeSlot))
        } returns "Bonjour Chris"

        val result = service.getTextWithPlaceHolder(code, args, localeTag)

        assertThat(result).isEqualTo("Bonjour Chris")
        assertThat(localeSlot.captured.language).isEqualTo("fr")
        assertThat(localeSlot.captured.country).isEqualTo("FR")

        verify(exactly = 1) { messageSource.getMessage(code, any<Array<Any>>(), any<Locale>()) }
    }

    @Test
    fun `getText propagates NoSuchMessageException when key missing`() {
        val key = "missing.key"
        val localeTag = "en"

        every { messageSource.getMessage(key, null, any<Locale>()) } throws NoSuchMessageException(key)

        val thrown = org.junit.jupiter.api.assertThrows<NoSuchMessageException> {
            service.getText(key, localeTag)
        }
        assertThat(thrown.message).contains(key)
    }
}
