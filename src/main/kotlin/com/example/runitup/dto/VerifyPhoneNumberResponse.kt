package com.example.runitup.dto

import com.example.runitup.model.User

class VerifyPhoneNumberResponse(val success:Boolean, val user: User?, val token: String?)