package com.example.runitup.mobile.rest.v1.dto
import java.time.LocalDate
import java.time.LocalTime

class CreateRunSessionRequest(
    val gymId:String,
    val date: String,
    var startTime: String,      // local time at the venue
    var endTime: String,      // local time at the venue
    var allowGuest: Boolean,
    var notes:String,
    var privateRun : Boolean,
    var description: String,
    var fee: Double = 0.0,
    var minPlayer: Int = 10,
    var maxPlayer: Int,
    var title: String,
    var maxGuest: Int,
    var createdBy:String?,
    var isAdmin:Boolean = true,
)