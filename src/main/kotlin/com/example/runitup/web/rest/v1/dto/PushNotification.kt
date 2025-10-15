package com.example.runitup.web.rest.v1.dto

data class PushNotification(
    val title: String?,
    val body: String?,
    val data: Map<String, String> = emptyMap(),
    val imageUrl: String? = null,
    val sound: String? = "default",
    val badge: Int? = null,
    val clickAction: String? = null
)
