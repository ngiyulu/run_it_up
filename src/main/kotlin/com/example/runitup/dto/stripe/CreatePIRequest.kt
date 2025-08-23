package com.example.runitup.dto.stripe

data class CreatePIRequest(
    val amount: Long,                   // minor units (e.g., 1099 = $10.99)
    var currency: String,
    var customerId: String? = null,
    val description: String? = null,
    val metadata: Map<String, String>? = null,
    val idempotencyKey: String? = null  // recommend passing from client/server to prevent dupes
)