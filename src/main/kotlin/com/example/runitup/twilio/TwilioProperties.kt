package com.example.runitup.twilio

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "twilio")
data class TwilioProperties @ConstructorBinding constructor(
    val accountSid: String,
    val authToken: String,
    val messagingServiceSid: String? = null,
    val fromNumber: String? = null,
    val enabled: Boolean = true
)