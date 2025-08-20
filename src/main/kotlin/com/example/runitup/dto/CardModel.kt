package com.example.runitup.dto

data class CardModel(
    val id: String,
    val brand: String?,
    val last4: String?,
    val expMonth: Int?,
    val expYear: Int?,
    var isDefault: Boolean
)