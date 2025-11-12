package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.model.User


class VerifyPhoneNumberResponse(val success:Boolean, val user: User?, val token: String?, val userStats: UserStat?)