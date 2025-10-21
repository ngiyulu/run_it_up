package com.example.runitup.mobile.rest.v1.dto.initialize

import com.example.runitup.mobile.model.User
import org.springframework.beans.factory.annotation.Value

class InitializeResponse(
    val gyms: List<com.example.runitup.mobile.model.Gym>,
    var user: User?,
    var token: String,
    var allowGuest: Boolean,
    var maxGuest: Int,
    var waiverUrl: String = "https://google.com",
    @Value("\${email}")
    var supportEmail: String = ""
)