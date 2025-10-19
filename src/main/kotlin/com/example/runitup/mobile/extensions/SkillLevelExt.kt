package com.example.runitup.mobile.extensions

import com.example.runitup.mobile.enum.SkillLevel

fun String.mapFromString(): SkillLevel {
    if(this == "BEGINNER"){
        return SkillLevel.BEGINNER
    }
    if(this == "COMPETITIVE"){
        return SkillLevel.COMPETITIVE
    }

    if(this == "COLLEGIATE"){
        return SkillLevel.COLLEGIATE
    }
    if(this == "PRO"){
        return SkillLevel.PRO
    }
    return SkillLevel.BEGINNER
}