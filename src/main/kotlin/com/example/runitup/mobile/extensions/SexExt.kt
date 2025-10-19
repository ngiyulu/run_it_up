package com.example.runitup.mobile.extensions

import com.ngiyulu.runitup.messaging.runitupmessaging.model.user.Sex

fun String.mapFromStringToSex(): Sex {
    if(this == "MALE"){
        return Sex.MALE
    }
    if(this == "FEMALE"){
        return Sex.FEMALE
    }

    if(this == "UNKNOWN"){
        return Sex.UNKNOWN
    }
    return Sex.UNDISCLOSED
}