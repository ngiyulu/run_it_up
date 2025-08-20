package com.example.runitup.dto.payment

import org.springframework.data.mongodb.core.mapping.Field

data class CardModel(
    val id: String,
    val brand: String?,
    val last4: String?,
    val expMonth: Int?,
    val expYear: Int?,
    @field:Field("isDefault")         // for Mongo field name
    var isDefault: Boolean
)