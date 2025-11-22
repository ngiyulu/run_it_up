package com.example.runitup.mobile.rest.v1.dto.session

class CancelSessionModel (
    val sessionId: String,
    val userId:String,
    val reason:String
)


class LeaveSessionModel (
    val sessionId: String)