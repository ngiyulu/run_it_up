package com.example.runitup.config

import com.sendgrid.SendGrid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader


@Configuration
class SendGridConfig {
    @Value("\${sendgrid.api.key}")
    private val sendGridApiKey: String? = null

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @Bean
    fun sendGrid(): SendGrid {
        return SendGrid(sendGridApiKey)
    }

    @Bean
    fun resourceLoader(): ResourceLoader {
        return resourceLoader
    }
}