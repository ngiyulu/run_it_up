// src/test/kotlin/com/example/runitup/mobile/service/PhoneServiceTest.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.PhoneType
import com.example.runitup.mobile.model.Phone
import com.example.runitup.mobile.repository.PhoneRepository
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import constant.HeaderConstants
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PhoneServiceTest {

    private lateinit var service: PhoneService
    private val repo = mockk<PhoneRepository>(relaxed = true)

    @BeforeEach
    fun setUp() {
        service = PhoneService().apply {
            this.phoneRepository = repo
        }
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `createPhone - when phone not found and token type is iOS, saves with PhoneType_IOS`() {
        val token = FirebaseTokenModel(
            token = "tkn-ios-1",
            phoneId = "pid-1",
            type = HeaderConstants.IOS_TYPE
        )
        every { repo.findByPhoneId("pid-1") } returns null

        val savedSlot = slot<Phone>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val res = service.createPhone(token, os = "iOS 17", userId = "u-1")

        // Repository calls
        verify(exactly = 1) { repo.findByPhoneId("pid-1") }
        verify(exactly = 1) { repo.save(any<Phone>()) }

        // Saved entity
        val saved = savedSlot.captured
        assertThat(saved.userId).isEqualTo("u-1")
        assertThat(saved.os).isEqualTo("iOS 17")
        assertThat(saved.phoneId).isEqualTo("pid-1")
        assertThat(saved.token).isEqualTo("tkn-ios-1")
        assertThat(saved.type).isEqualTo(PhoneType.IOS)

        // Returned entity is the same as repo.save return
        assertThat(res).isSameAs(saved)
    }

    @Test
    fun `createPhone - when phone not found and token type is NOT iOS, saves with PhoneType_ANDROID`() {
        val token = FirebaseTokenModel(
            token = "tkn-and-1",
            phoneId = "pid-2",
            type = "ANDROID" // anything not equal to HeaderConstants.IOS_TYPE should produce ANDROID
        )
        every { repo.findByPhoneId("pid-2") } returns null

        val savedSlot = slot<Phone>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val res = service.createPhone(token, os = "Android 15", userId = "u-2")

        verify(exactly = 1) { repo.findByPhoneId("pid-2") }
        verify(exactly = 1) { repo.save(any<Phone>()) }

        val saved = savedSlot.captured
        assertThat(saved.userId).isEqualTo("u-2")
        assertThat(saved.os).isEqualTo("Android 15")
        assertThat(saved.phoneId).isEqualTo("pid-2")
        assertThat(saved.token).isEqualTo("tkn-and-1")
        assertThat(saved.type).isEqualTo(PhoneType.ANDROID)

        assertThat(res).isSameAs(saved)
    }

    @Test
    fun `createPhone - when phone exists and token is the same, does not save again`() {
        val existing = Phone(
            os = "Android 14",
            model = "Pixel",
            userId = "u-3",
            token = "same-token",
            phoneId = "pid-3",
            type = PhoneType.ANDROID
        )
        every { repo.findByPhoneId("pid-3") } returns existing

        val res = service.createPhone(
            token = FirebaseTokenModel(token = "same-token", phoneId = "pid-3", type = "ANDROID"),
            os = "Android 15 (ignored)",
            userId = "u-ignored"
        )

        verify(exactly = 1) { repo.findByPhoneId("pid-3") }
        verify(exactly = 0) { repo.save(any<Phone>()) } // no update/save

        // Should return the same existing instance
        assertThat(res).isSameAs(existing)
        assertThat(res.token).isEqualTo("same-token")
        assertThat(res.phoneId).isEqualTo("pid-3")
    }

    @Test
    fun `createPhone - when phone exists and token changes, updates token and saves`() {
        val existing = Phone(
            os = "Android 14",
            model = "Galaxy",
            userId = "u-4",
            token = "old-token",
            phoneId = "old-pid",
            type = PhoneType.ANDROID
        )
        every { repo.findByPhoneId("pid-4") } returns existing

        val savedSlot = slot<Phone>()
        every { repo.save(capture(savedSlot)) } answers { firstArg() }

        val res = service.createPhone(
            token = FirebaseTokenModel(token = "new-token", phoneId = "pid-4", type = "ANDROID"),
            os = "Android 15 (ignored)",
            userId = "u-ignored"
        )

        verify(exactly = 1) { repo.findByPhoneId("pid-4") }
        verify(exactly = 1) { repo.save(any<Phone>()) } // updated

        val saved = savedSlot.captured
        assertThat(saved.token).isEqualTo("new-token")
        assertThat(saved.phoneId).isEqualTo("pid-4")
        // ensure it’s the same object reference updated
        assertThat(saved).isSameAs(existing)
        // return value is the (updated) same instance
        assertThat(res).isSameAs(existing)
    }

    @Test
    fun `deletePhone - when found, deletes entity`() {
        val existing = Phone(
            os = "iOS 17",
            model = "iPhone",
            userId = "u-5",
            token = "abc",
            phoneId = "pid-5",
            type = PhoneType.IOS
        )
        every { repo.findByPhoneId("pid-5") } returns existing
        every { repo.delete(existing) } just Runs

        service.deletePhone(FirebaseTokenModel(token = "any", phoneId = "pid-5", type = HeaderConstants.IOS_TYPE))

        verify(exactly = 1) { repo.findByPhoneId("pid-5") }
        verify(exactly = 1) { repo.delete(existing) }
    }

    @Test
    fun `deletePhone - when not found, no-op`() {
        every { repo.findByPhoneId("missing") } returns null

        service.deletePhone(FirebaseTokenModel(token = "x", phoneId = "missing", type = "ANDROID"))

        verify(exactly = 1) { repo.findByPhoneId("missing") }
        verify(exactly = 0) { repo.delete(any<Phone>()) }
    }
}
