package com.example.runitup.config

import com.example.runitup.twilio.TwilioProperties
import com.twilio.Twilio
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(TwilioProperties::class)
class TwilioInitializer{

    @Autowired
   lateinit var props: TwilioProperties
    @PostConstruct
    fun init(): Any {
        // Initialize Twilio globally once
        Twilio.init(props.accountSid, props.authToken)
        return Any()
    }
}