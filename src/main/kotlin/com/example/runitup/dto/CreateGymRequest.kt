package com.example.runitup.dto

import com.example.runitup.model.Gym

data class CreateGymRequest(
    val gym: Gym,
    val longitude: Double,
    val latitude: Double
)
