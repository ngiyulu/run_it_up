package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.model.User


class VerifyUserResponse (val flow: VerifyUserFlow, val user: User?, val token: String?, val userId: String, val phone: String)

enum class VerifyUserFlow{
    CREATE, LOGIN
}