package com.example.runitup.mobile.service

import com.example.runitup.mobile.config.EncryptionConfig
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.dto.EncryptedCodeModel
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


@Service
class NumberGenerator(
    private val config: EncryptionConfig) {


    private val secureRandom = SecureRandom()
    fun generateCode(length: Long): String{
        val code: String = ThreadLocalRandom.current()
            .ints(length, 0, 10)
            .mapToObj(java.lang.String::valueOf)
            .collect(Collectors.joining())
        return code
    }

    fun encryptCode(length: Long): EncryptedCodeModel {
        val code = generateCode(length)
        val (iv, ciphertext, tag) = encryptAesGcm(code.toByteArray(Charsets.UTF_8))
        return EncryptedCodeModel(
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            tag = Base64.getEncoder().encodeToString(tag)
        )
    }

    // returns Triple(iv, ciphertextWithoutTag, tag)
    private fun encryptAesGcm(plain: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        val iv = ByteArray(12) // recommended 12 bytes for GCM
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv) // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, config.secretKeySpec(), gcmSpec)
        val cipherOutput = cipher.doFinal(plain) // ciphertext || tag (tag appended)

        // split ciphertext and tag (tag = last 16 bytes)
        val tagLen = 16
        val ctLen = cipherOutput.size - tagLen
        val ciphertext = cipherOutput.copyOfRange(0, ctLen)
        val tag = cipherOutput.copyOfRange(ctLen, cipherOutput.size)
        return Triple(iv, ciphertext, tag)
    }

    fun validateEncryptedCode(
        response: EncryptedCodeModel,
        expectedCode: String
    ): Boolean {
        // decode base64 parts
        val iv = Base64.getDecoder().decode(response.iv)
        val ciphertext = Base64.getDecoder().decode(response.ciphertext)
        val tag = Base64.getDecoder().decode(response.tag)

        // combine ciphertext + tag (as produced by Cipher.doFinal)
        val combined = ByteArray(ciphertext.size + tag.size)
        System.arraycopy(ciphertext, 0, combined, 0, ciphertext.size)
        System.arraycopy(tag, 0, combined, ciphertext.size, tag.size)

        val keyBytes = Base64.getDecoder().decode(config.getKey())
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        return try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val plainBytes = cipher.doFinal(combined)
            val decrypted = String(plainBytes, Charsets.UTF_8)
            decrypted == expectedCode
        } catch (e: Exception) {
            myLogger().error("validateEncryptedCode faield ${e.message.orEmpty()}")
            // Decryption failed — invalid key, tampered data, or mismatched code
            false
        }
    }
}