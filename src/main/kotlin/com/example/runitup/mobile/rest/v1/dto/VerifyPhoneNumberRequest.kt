package com.example.runitup.mobile.rest.v1.dto

class VerifyPhoneNumberRequest(val otp: String, val phoneNumber: String, val tokenModel: FirebaseTokenModel?)