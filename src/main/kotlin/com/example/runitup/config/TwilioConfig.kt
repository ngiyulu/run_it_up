package com.example.runitup.config

import com.twilio.Twilio
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TwilioInitializer(
    @Value("\${twilio.account.sid}") private val sid: String,
    @Value("\${twilio.auth.token}") private val token: String
) {
    @PostConstruct
    fun init() {
        Twilio.init(sid, token)
    }
}