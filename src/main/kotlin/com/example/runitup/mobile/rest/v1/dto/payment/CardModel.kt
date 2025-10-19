package com.example.runitup.mobile.rest.v1.dto.payment

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.mongodb.core.mapping.Field

data class CardModel(
    val id: String,
    val brand: String?,
    val last4: String?,
    val expMonth: Int?,
    val expYear: Int?,
    @field:JsonProperty("isDefault")  // for Jackson (write/read)
    @get:JsonProperty("isDefault")    // sometimes needed for getters
    @field:Field("isDefault")           // for Mongo field name
    var isDefault: Boolean
)