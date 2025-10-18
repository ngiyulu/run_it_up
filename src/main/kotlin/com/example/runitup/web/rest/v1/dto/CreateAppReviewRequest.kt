package com.example.runitup.web.rest.v1.dto

data class CreateAppReviewRequest(
    val star:Int,
    var feedback: String
)