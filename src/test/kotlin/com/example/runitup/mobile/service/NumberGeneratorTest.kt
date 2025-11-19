package com.example.runitup.mobile.service

import com.example.runitup.mobile.config.EncryptionConfig
import com.example.runitup.mobile.rest.v1.dto.EncryptedCodeModel
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.util.Base64
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class NumberGeneratorTest {

    private lateinit var config: EncryptionConfig
    private lateinit var generator: NumberGenerator
    private lateinit var base64Key: String
    private lateinit var secretKey: SecretKeySpec

    @BeforeEach
    fun setUp() {
        // Fixed AES-256 key (32 bytes) so tests are deterministic
        val keyBytes = ByteArray(32) { 7 } // any constant value
        base64Key = Base64.getEncoder().encodeToString(keyBytes)
        secretKey = SecretKeySpec(keyBytes, "AES")

        config = mockk(relaxed = true)

        every { config.getKey() } returns base64Key
        every { config.secretKeySpec() } returns secretKey

        generator = NumberGenerator(config)
    }

    // -------------------------------------------------------------------------
    // generateCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateCode returns numeric string of requested length")
    fun generateCode_numericWithCorrectLength() {
        val length = 6L

        val code = generator.generateCode(length)

        assertThat(code).hasSize(length.toInt())
        assertThat(code).matches(Pattern.compile("\\d{6}"))
    }

    @Test
    @DisplayName("generateCode returns different codes across multiple calls (likely)")
    fun generateCode_probabilisticallyDifferent() {
        val length = 4L

        val codes = (1..5).map { generator.generateCode(length) }

        // All numeric and correct length
        codes.forEach {
            assertThat(it).hasSize(length.toInt())
            assertThat(it).matches(Pattern.compile("\\d{4}"))
        }

        // It’s probabilistic, but extremely unlikely all 5 are identical
        assertThat(codes.distinct().size).isGreaterThan(1)
    }

    // -------------------------------------------------------------------------
    // encryptCode + decryptEncryptedCode (roundtrip)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("encryptCode + decryptEncryptedCode roundtrip yields original-length numeric code")
    fun encryptAndDecrypt_roundtrip() {
        val length = 5L

        val encrypted = generator.encryptCode(length)
        val decrypted = generator.decryptEncryptedCode(encrypted)

        assertThat(decrypted).isNotNull
        assertThat(decrypted!!.length).isEqualTo(length.toInt())
        assertThat(decrypted).matches(Pattern.compile("\\d{5}"))
    }

    @Test
    @DisplayName("encryptCode produces non-empty, valid base64 parts")
    fun encryptCode_producesBase64Fields() {
        val encrypted = generator.encryptCode(4)

        // All fields non-blank
        assertThat(encrypted.iv).isNotBlank
        assertThat(encrypted.ciphertext).isNotBlank
        assertThat(encrypted.tag).isNotBlank

        // All decode from Base64 successfully and are non-empty
        val ivBytes = Base64.getDecoder().decode(encrypted.iv)
        val ctBytes = Base64.getDecoder().decode(encrypted.ciphertext)
        val tagBytes = Base64.getDecoder().decode(encrypted.tag)

        assertThat(ivBytes.size).isGreaterThan(0)
        assertThat(ctBytes.size).isGreaterThan(0)
        assertThat(tagBytes.size).isGreaterThan(0)
    }

    // -------------------------------------------------------------------------
    // validateEncryptedCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateEncryptedCode returns true for valid encrypted payload and expected code")
    fun validateEncryptedCode_validPayload_returnsTrue() {
        val code = "123456"

        val model = createEncryptedModelForCode(code, base64Key)

        val isValid = generator.validateEncryptedCode(model, code)

        assertThat(isValid).isTrue()
    }

    @Test
    @DisplayName("validateEncryptedCode returns false when expected code does not match decrypted value")
    fun validateEncryptedCode_wrongExpectedCode_returnsFalse() {
        val realCode = "9999"
        val wrongCode = "0000"

        val model = createEncryptedModelForCode(realCode, base64Key)

        val isValid = generator.validateEncryptedCode(model, wrongCode)

        assertThat(isValid).isFalse()
    }

    @Test
    @DisplayName("validateEncryptedCode returns false when ciphertext is tampered")
    fun validateEncryptedCode_tamperedCiphertext_returnsFalse() {
        val code = "4321"
        val model = createEncryptedModelForCode(code, base64Key)

        // Tamper with ciphertext bytes
        val ctBytes = Base64.getDecoder().decode(model.ciphertext)
        ctBytes[0] = (ctBytes[0].toInt() xor 0xFF).toByte()
        val tamperedModel = model.copy(
            ciphertext = Base64.getEncoder().encodeToString(ctBytes)
        )

        val isValid = generator.validateEncryptedCode(tamperedModel, code)

        assertThat(isValid).isFalse()
    }

    // -------------------------------------------------------------------------
    // decryptEncryptedCode
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("decryptEncryptedCode returns original code for valid payload")
    fun decryptEncryptedCode_validPayload_returnsCode() {
        val code = "2025"

        val model = createEncryptedModelForCode(code, base64Key)

        val decrypted = generator.decryptEncryptedCode(model)

        assertThat(decrypted).isEqualTo(code)
    }

    @Test
    @DisplayName("decryptEncryptedCode returns null for tampered data")
    fun decryptEncryptedCode_tampered_returnsNull() {
        val code = "5555"
        val model = createEncryptedModelForCode(code, base64Key)

        val tagBytes = Base64.getDecoder().decode(model.tag)
        tagBytes[0] = (tagBytes[0].toInt() xor 0x0F).toByte()

        val tamperedModel = model.copy(
            tag = Base64.getEncoder().encodeToString(tagBytes)
        )

        val decrypted = generator.decryptEncryptedCode(tamperedModel)

        assertThat(decrypted).isNull()
    }

    // -------------------------------------------------------------------------
    // Helper: build an EncryptedCodeModel using same algorithm as NumberGenerator
    // -------------------------------------------------------------------------

    /**
     * Builds an [EncryptedCodeModel] for a given [code] using AES/GCM with the same
     * conventions as NumberGenerator:
     *
     * - AES/GCM/NoPadding
     * - 12-byte IV
     * - 16-byte tag appended to ciphertext from Cipher.doFinal
     * - Base64 encoding of iv, ciphertext, tag
     */
    private fun createEncryptedModelForCode(code: String, keyBase64: String): EncryptedCodeModel {
        val keyBytes = Base64.getDecoder().decode(keyBase64)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val plainBytes = code.toByteArray(Charsets.UTF_8)
        val cipherOut = cipher.doFinal(plainBytes) // ciphertext || tag

        val tagLen = 16
        val ctLen = cipherOut.size - tagLen
        val ciphertext = cipherOut.copyOfRange(0, ctLen)
        val tag = cipherOut.copyOfRange(ctLen, cipherOut.size)

        return EncryptedCodeModel(
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            tag = Base64.getEncoder().encodeToString(tag)
        )
    }
}
