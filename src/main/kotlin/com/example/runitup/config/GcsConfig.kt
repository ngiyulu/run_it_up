package com.example.runitup.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.*


@Configuration
@EnableConfigurationProperties(com.example.runitup.web.rest.v1.dto.GcsProps::class)
class GcsConfig {

    @Value("\${firebase}") private val firebaseBase64: String = ""
   private val projectId: String = "runitup-94575"
    @Bean
    fun storage(props: com.example.runitup.web.rest.v1.dto.GcsProps): Storage {
        val builder = StorageOptions.newBuilder().setProjectId(projectId)
        val credsBytes = Base64.getDecoder().decode(firebaseBase64)
        ByteArrayInputStream(credsBytes).use { input ->
            val creds = GoogleCredentials.fromStream(input)
            builder.setCredentials(creds)
        }
        return builder.build().service
    }
}

