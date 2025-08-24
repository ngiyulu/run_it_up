package com.example.runitup.web.rest.v1.dto

import com.example.runitup.model.Gym

data class CreateGymRequest(
    val gym: Gym,
    val longitude: Double,
    val latitude: Double
)
