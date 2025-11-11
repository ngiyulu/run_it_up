package com.example.runitup.mobile.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.util.*
import javax.crypto.spec.SecretKeySpec


@Configuration
class EncryptionConfig(
    @Value("\${app.encryption.key}") private val base64Key: String
) {
    // Returns the raw 32 byte AES key
    val aesKey: ByteArray = Base64.getDecoder().decode(base64Key).also {
        require(it.size == 32) { "app.encryption.key must be 32 bytes (base64-encoded 256-bit key)" }
    }
    fun secretKeySpec() = SecretKeySpec(aesKey, "AES")

    fun getKey() = base64Key
}

