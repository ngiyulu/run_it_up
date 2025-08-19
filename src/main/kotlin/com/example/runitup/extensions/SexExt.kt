package com.example.runitup.extensions

import com.example.runitup.enum.Sex

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