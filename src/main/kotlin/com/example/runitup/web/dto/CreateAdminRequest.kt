package com.example.runitup.web.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateAdminRequest(
    @field:Email val email: String,
    @field:NotBlank val password: String,
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank val phoneNumber: String,
)
