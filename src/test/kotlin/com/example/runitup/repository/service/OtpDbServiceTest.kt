// src/test/kotlin/com/example/runitup/mobile/repository/service/OtpDbServiceTest.kt
package com.example.runitup.repository.service

import com.example.runitup.mobile.model.Otp
import com.example.runitup.mobile.repository.OtpRepository
import com.example.runitup.mobile.repository.service.OtpDbService
import com.example.runitup.mobile.service.NumberGenerator
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant
import java.util.regex.Pattern

class OtpDbServiceTest {

    private val mongoTemplate = mockk<MongoTemplate>(relaxed = true)
    private val otpRepository = mockk<OtpRepository>(relaxed = true)
    private val numberGenerator = mockk<NumberGenerator>()
    private lateinit var service: OtpDbService

    @BeforeEach
    fun setUp() {
        service = OtpDbService().apply {
            this.mongoTemplate = this@OtpDbServiceTest.mongoTemplate
            this.otpRepository = this@OtpDbServiceTest.otpRepository
            this.numberGenerator = this@OtpDbServiceTest.numberGenerator
        }
        clearMocks(mongoTemplate, otpRepository, numberGenerator)
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `getOtp delegates to repository`() {
        val phone = "+15551230000"
        val expected = Otp(
            code = "1234",
            userId = "u1",
            phoneNumber = phone
        ).apply {
            this.isActive = true
            createdAt = Instant.now()
        }

        every { otpRepository.findByPhoneNumberAndIsActive(phone, true) } returns expected

        val res = service.getOtp(phone)

        assertThat(res).isSameAs(expected)
        verify(exactly = 1) { otpRepository.findByPhoneNumberAndIsActive(phone, true) }
        confirmVerified(otpRepository)
    }

    @Test
    fun `generateOtp deactivates previous and saves new 4-digit code`() {
        val userId = "u42"
        val phone = "+15557654321"

        // Stub numberGenerator to produce a deterministic code
        every { numberGenerator.generateCode(4) } returns "5678"

        // capture query & update
        val querySlot = slot<Query>()
        val updateSlot = slot<Update>()
        every {
            mongoTemplate.updateFirst(capture(querySlot), capture(updateSlot), Otp::class.java)
        } returns mockk(relaxed = true)

        // capture saved Otp
        val savedSlot = slot<Otp>()
        every { otpRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val created = service.generateOtp(userId, phone)

        // verify deactivation call
        verify(exactly = 1) {
            mongoTemplate.updateFirst(any<Query>(), any<Update>(), Otp::class.java)
        }
        // verify save called once
        verify(exactly = 1) { otpRepository.save(any<Otp>()) }
        // verify code generator used
        verify(exactly = 1) { numberGenerator.generateCode(4) }

        val saved = savedSlot.captured
        assertThat(saved.userId).isEqualTo(userId)
        assertThat(saved.phoneNumber).isEqualTo(phone)
        // createdAt might be set by @CreatedDate in real DB; in unit test it may be null,
        // so only assert code-related behavior here.
        assertThat(saved.code)
            .hasSize(4)
            .matches(Pattern.compile("\\d{4}"))
        assertThat(saved.code).isEqualTo("5678")

        // ensure returned value matches what was saved
        assertThat(created).isSameAs(saved)
    }

    @Test
    fun `disableOtp flips isActive to false and saves`() {
        val otp = Otp(
            code = "9876",
            userId = "u9",
            phoneNumber = "+15550009999"
        ).apply {
            this.isActive = true
            createdAt = Instant.now()
        }

        val savedSlot = slot<Otp>()
        every { otpRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val out = service.disableOtp(otp)

        assertThat(savedSlot.captured.isActive).isFalse()
        assertThat(out.isActive).isFalse()
        verify(exactly = 1) { otpRepository.save(any<Otp>()) }
    }
}
