package com.example.runitup.mobile.rest.v1.dto
import java.time.LocalDate
import java.time.LocalTime

class CreateRunSessionRequest(
    val gymId:String,
    val date: LocalDate,
    var startTime: LocalTime,      // local time at the venue
    var endTime: LocalTime,      // local time at the venue
    val zoneId: String,       // IANA zone, e.g. "America/Chicago"
    var allowGuest: Boolean,
    var notes:String,
    var privateRun : Boolean,
    var description: String,
    var fee: Double = 0.0,
    var minPlayer: Int = 10,
    var maxPlayer: Int,
    var title: String,
    var maxGuest: Int,
)