package com.example.runitup.mobile.rest.v1.dto

class JoinRunSessionResponse(val status: JoinRunSessionStatus, val runSession: com.example.runitup.mobile.model.RunSession)

enum class JoinRunSessionStatus{
    FULL,
    GUEST_FULL,
    WAITLISTED,
    ALREADY_BOOKED,
    NONE
}