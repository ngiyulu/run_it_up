package com.example.runitup.mobile.rest.v1.dto

data class PushJobModel(
    val type:PushJobType,
    val dataId:String,
    val metadata: HashMap<String, String> = HashMap()
)

enum class PushJobType{
    CONFIRM_RUN, CANCEL_RUN, USER_JOINED, BOOKING_CANCELLED
}