package com.example.runitup.mobile.rest.v1.dto

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
    // this is manual fee when payment feature is not enable, we can manually track who paid
    // and who didn't
    var manualFee:Double = 0.0,
    var minPlayer: Int = 10,
    var maxPlayer: Int,
    var title: String,
    var maxGuest: Int,
    var createdBy:String?,
    var isAdmin:Boolean = true,
)