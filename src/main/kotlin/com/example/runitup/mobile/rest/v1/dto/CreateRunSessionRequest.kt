package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.model.RunSession

class CreateRunSessionRequest(
    val gymId:String,
    val runSession: RunSession)