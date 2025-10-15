package com.example.runitup.push

import com.example.runitup.web.rest.v1.dto.PushNotification
import com.example.runitup.web.rest.v1.dto.PushResult

interface PushGateway {
    fun sendToTokens(tokens: List<String>, notif: PushNotification): PushResult
}