package com.example.runitup.mobile.clicksend

// Model the exact bits we need back
data class SendResult(
    val accepted: Boolean,
    val responseMsg: String?,
    val providerStatus: String?,
    val messageId: String?
)
