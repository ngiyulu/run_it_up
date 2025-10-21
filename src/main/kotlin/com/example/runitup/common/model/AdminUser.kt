package com.example.runitup.common.model

import com.example.runitup.web.dto.Role
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Id

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdminUser(
    @Id val id: String? = null,
    val email: String = "",
    val first: String = "",
    var last: String = "",
    val passwordHash: String = "",
    val role: Role = Role.ADMIN,
    val enabled: Boolean = true
){
    fun getName(): String{
        return "$first ${last}"
    }
}