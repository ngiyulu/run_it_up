package com.example.runitup.mobile.rest.v1.dto.session

data class JoinWaitListModel(
    val sessionId: String,
    val entry:String = "Join WaitList API",
    val paymentMethodId:String){
}