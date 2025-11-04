// src/test/kotlin/com/example/runitup/mobile/extensions/ExtensionsTest.kt
package com.example.runitup.mobile.extensions

import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.stripe.model.PaymentMethod
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExtensionsTest {

    @Nested
    inner class MapToUserPaymentTest {
        @Test
        fun `maps PaymentMethod card details to CardModel`() {
            val card = mockk<PaymentMethod.Card>()
            every { card.brand } returns "visa"
            every { card.last4 } returns "4242"
            every { card.expMonth } returns 12L
            every { card.expYear } returns 2030L

            val pm = mockk<PaymentMethod>()
            every { pm.id } returns "pm_123"
            every { pm.card } returns card

            val result: CardModel = pm.mapToUserPayment(isDefaultPayment = true)

            assertThat(result.id).isEqualTo("pm_123")
            assertThat(result.brand).isEqualTo("visa")
            assertThat(result.last4).isEqualTo("4242")
            assertThat(result.expMonth).isEqualTo(12)
            assertThat(result.expYear).isEqualTo(2030)
            assertThat(result.isDefault).isTrue()
        }
    }

    @Nested
    inner class ConvertToCentsTest {
        @Test
        fun `10_99 dollars converts to 1099 cents`() {
            // Intended behavior per comment in code
            assertThat(10.99.convertToCents()).isEqualTo(1099)
        }

        @Test
        fun `10_9 dollars converts to 1090 cents`() {
            assertThat(10.9.convertToCents()).isEqualTo(1090)
        }

        @Test
        fun `round half up below midpoint - 10_994 to 1099 cents`() {
            assertThat(10.994.convertToCents()).isEqualTo(1099)
        }

        @Test
        fun `round half up at midpoint - 10_995 to 1100 cents`() {
            assertThat(10.995.convertToCents()).isEqualTo(1100)
        }
    }

    @Nested
    inner class LetOverloadsTest {
        @Test
        fun `let(A,B) returns block result when both non-null`() {
            val a: String? = "Hi"
            val b: Int? = 3
            val res = let(a, b) { x, y -> "$x-$y" }
            assertThat(res).isEqualTo("Hi-3")
        }

        @Test
        fun `let(A,B) returns null when any is null`() {
            val a: String? = null
            val b: Int? = 3
            val res = let(a, b) { x, y -> "$x-$y" }
            assertThat(res).isNull()
        }

        @Test
        fun `let(A,B,C) returns block result when all non-null`() {
            val a: String? = "A"
            val b: String? = "B"
            val c: String? = "C"
            val res = let(a, b, c) { x, y, z -> x + y + z }
            assertThat(res).isEqualTo("ABC")
        }

        @Test
        fun `let(A,B,C) returns null when any is null`() {
            val a: String? = "A"
            val b: String? = null
            val c: String? = "C"
            val res = let(a, b, c) { x, y, z -> x + y + z }
            assertThat(res).isNull()
        }
    }
}
