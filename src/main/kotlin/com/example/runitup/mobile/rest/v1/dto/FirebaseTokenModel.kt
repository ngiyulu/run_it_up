package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.enum.PushRegistrationEnvironment

class FirebaseTokenModel(val token:String, val phoneId:String, var type:String? = null, val model:String ? = null, val environment: PushRegistrationEnvironment? = null )