package com.example.runitup.dto.initialize

import com.example.runitup.model.Gym
import com.example.runitup.model.User

class InitializeResponse(
    val gyms: List<Gym>,
    var user: User?,
    var token: String,
    var maxGuest: Int
)