package com.example.runitup.mobile.push

import com.example.runitup.mobile.rest.v1.dto.PushNotification
import com.example.runitup.mobile.rest.v1.dto.PushResult


interface PushGateway {
    fun sendToTokens(tokens: List<String>, notif: PushNotification): PushResult
}