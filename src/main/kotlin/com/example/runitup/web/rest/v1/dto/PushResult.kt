package com.example.runitup.web.rest.v1.dto

data class PushResult(
    val requested: Int,
    val success: Int,
    val failed: Int,
    val invalidTokens: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)
