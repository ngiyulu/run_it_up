package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.model.RunSession

class JoinRunSessionResponse(val status: JoinRunSessionStatus, val runSession: com.example.runitup.mobile.model.RunSession)

enum class JoinRunSessionStatus{
    FULL,
    GUEST_FULL,
    ALREADY_BOOKED,
    NONE
}