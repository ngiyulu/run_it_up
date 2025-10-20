package com.example.runitup.mobile.clicksend

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clicksend")
data class ClickSendProperties(
    val username: String,
    val apiKey: String,
    val senderId: String? = null,
    val enabled: Boolean = true,
    val baseUrl: String = "https://rest.clicksend.com/v3",
    val timeoutMs: Long = 5000
)