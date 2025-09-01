package com.example.runitup.web.rest.v1.dto

import com.example.runitup.model.RunSession

class JoinRunSessionResponse(val status: JoinRunSessionStatus, val runSession: RunSession)

enum class JoinRunSessionStatus{
    FULL,
    GUEST_FULL,
    ALREADY_BOOKED,
    NONE
}