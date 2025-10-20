package com.example.runitup.mobile.rest.v1.dto.stripe

data class CreatePIResponse(
    val paymentIntentId: String,
    val clientSecret: String,
    val amount: Long,
    val currency: String
)