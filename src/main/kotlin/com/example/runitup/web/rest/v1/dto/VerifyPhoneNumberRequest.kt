package com.example.runitup.web.rest.v1.dto

import com.twilio.type.PhoneNumber

class VerifyPhoneNumberRequest(val otp: String, val phoneNumber: String, val tokenModel: FirebaseTokenModel?)