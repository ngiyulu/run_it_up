package com.example.runitup.model

data class QueueMessage(
    val id: String,                 // message id
    val body: String,               // payload (JSON/text)
    val attributes: Map<String, String> = emptyMap(),
    val delaySeconds: Int = 0,
    var receiveCount: Int = 0,      // increment on every delivery
)