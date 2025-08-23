package com.example.runitup.dto.stripe

data class CreatePIResponse(
    val paymentIntentId: String,
    val clientSecret: String,
    val amount: Long,
    val currency: String
)