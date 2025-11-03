package com.example.runitup.mobile.rest.v1.dto.queue

data class ReceiveRequest(
    val queue: String,
    val maxNumberOfMessages: Int = 1,
    val waitSeconds: Int? = null,
    val visibilitySeconds: Int? = null
)
