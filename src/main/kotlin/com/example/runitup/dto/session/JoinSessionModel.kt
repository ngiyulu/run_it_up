package com.example.runitup.dto.session

class JoinSessionModel(
    val sessionId: String,
    val paymentMethodId:String,
    val guest: Int){

    fun getTotalParticipants(): Int{
        return  guest + 1
    }
}