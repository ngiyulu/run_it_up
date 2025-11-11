package com.example.runitup.mobile.rest.v1.dto


data class EncryptedCodeModel(
    val iv: String,         // base64 iv (12 bytes)
    val ciphertext: String, // base64 ciphertext (no tag)
    val tag: String         // base64 GCM auth tag (16 bytes)
)