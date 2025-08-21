package com.example.runitup.config

import com.example.runitup.dto.GcsProps
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.*


@Configuration
@EnableConfigurationProperties(GcsProps::class)
class GcsConfig {

    @Value("\${firebase}") private val firebaseBase64: String = ""
   private val projectId: String = "runitup-94575"
    @Bean
    fun storage(props: GcsProps): Storage {
        val builder = StorageOptions.newBuilder().setProjectId(projectId)
        val credsBytes = Base64.getDecoder().decode(firebaseBase64)
        ByteArrayInputStream(credsBytes).use { input ->
            val creds = GoogleCredentials.fromStream(input)
            builder.setCredentials(creds)
        }
        return builder.build().service
    }
}

