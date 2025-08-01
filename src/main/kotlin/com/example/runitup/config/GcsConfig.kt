package com.example.runitup.config

import com.example.runitup.dto.GcsProps
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream


@Configuration
@EnableConfigurationProperties(GcsProps::class)
class GcsConfig {
    @Bean
    fun storage(props: GcsProps): Storage {
        val builder = StorageOptions.newBuilder().setProjectId(props.projectId)
        props.credentialsPath?.takeIf { it.isNotBlank() }?.let { path ->
            FileInputStream(path).use { builder.setCredentials(GoogleCredentials.fromStream(it)) }
        }
        return builder.build().service
    }
}

