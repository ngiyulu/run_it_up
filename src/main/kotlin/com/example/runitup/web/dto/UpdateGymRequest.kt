package com.example.runitup.web.dto

class UpdateGymRequest (
    val title:String,
    val gymId:String,
    val note:String,
    val description:String,
    var fee: Double,
    var phoneNumber: String? )