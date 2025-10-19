package com.example.runitup.common.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

data class AdminUser(
    @Id val id: String? = null,
    val email: String = "",
    val first: String = "",
    var last: String = "",
    val passwordHash: String = "",
    val enabled: Boolean = true
){
    fun getName(): String{
        return "$first ${last}"
    }
}