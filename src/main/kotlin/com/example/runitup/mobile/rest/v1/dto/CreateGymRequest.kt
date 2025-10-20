package com.example.runitup.mobile.rest.v1.dto

import org.springframework.web.multipart.MultipartFile

data class CreateGymRequest(
    val payloadRaw: String,
    val image: MultipartFile?,
    val id: String
)
