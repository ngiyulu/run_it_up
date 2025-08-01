package com.example.runitup.dto

// GcsProps.kt
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("gcs")
data class GcsProps(
    val projectId: String,
    val bucket: String,
    val credentialsPath: String? = null
)
