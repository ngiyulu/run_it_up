package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.SetupStatus

class JoinWaitListResponse(
    val success: Boolean,
    val clientKey:String?,
    val runSession: RunSession,
    val showSCA:Boolean?,
    val refresh:Boolean = false
)