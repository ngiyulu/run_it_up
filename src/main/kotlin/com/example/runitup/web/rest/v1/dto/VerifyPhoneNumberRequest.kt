package com.example.runitup.web.rest.v1.dto

class VerifyPhoneNumberRequest(val otp: String, val phoneNumber: String, val tokenModel: FirebaseTokenModel?)