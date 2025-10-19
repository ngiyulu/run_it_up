package com.example.runitup.mobile.rest.v1.dto

data class CreateAppReviewRequest(
    val star:Int,
    var feedback: String
)