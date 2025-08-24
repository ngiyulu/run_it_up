package com.example.runitup.web.rest.v1.dto

import com.example.runitup.model.User

class VerifyPhoneNumberResponse(val success:Boolean, val user: User?, val token: String?)