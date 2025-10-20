package com.example.runitup.web.dto

class GymCreateDto(
    val title:String = "",
    val address: GymCreateDtoAddress? = null,
    val fee: Double = 0.0 ,
    val phoneNumber: String = "",
    val notes: String = "",
    val description: String = "",
    val zipCode: Long = 0L,
)
class GymCreateDtoAddress(
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = ""
)