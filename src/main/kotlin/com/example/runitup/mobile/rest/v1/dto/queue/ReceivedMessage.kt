package com.example.runitup.mobile.rest.v1.dto.queue

data class ReceivedMessage(
    val messageId: String,
    val body: String,
    val attributes: Map<String, String>,
    val receiptHandle: String
)