package com.example.runitup.web.rest.v1.dto.initialize

import com.example.runitup.model.Gym
import com.example.runitup.model.User

class InitializeResponse(
    val gyms: List<Gym>,
    var user: User?,
    var token: String,
    var allowGuest: Boolean,
    var maxGuest: Int,
    var waiverUrl: String = "https://google.com"
)