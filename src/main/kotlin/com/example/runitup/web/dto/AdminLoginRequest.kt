package com.example.runitup.web.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(@field:Email val email: String, @field:NotBlank val password: String)
