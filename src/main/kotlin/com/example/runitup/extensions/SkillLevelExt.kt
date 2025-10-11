package com.example.runitup.extensions

import com.example.runitup.enum.SkillLevel

fun String.mapFromString(): SkillLevel{
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