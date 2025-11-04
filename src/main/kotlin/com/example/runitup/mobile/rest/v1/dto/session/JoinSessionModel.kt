package com.example.runitup.mobile.rest.v1.dto.session

data class JoinSessionModel(
    val sessionId: String,
    val paymentMethodId:String?,
    // guest only include guest, to get the whole party size, it's guest +1
    val guest: Int){

    fun getTotalParticipants(): Int{
        return  guest + 1
    }
}