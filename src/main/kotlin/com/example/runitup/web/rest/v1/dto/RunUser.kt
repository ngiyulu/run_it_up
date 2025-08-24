package com.example.runitup.web.rest.v1.dto

import com.example.runitup.enum.SkillLevel
import com.example.runitup.utility.AppUtil

class RunUser(
    val first: String,
    val last: String,
    var level: SkillLevel?,
    val userId: String?,
    var imageUrl:String?,
    var checkIn: Int = 0,
    var guest: Int,
    var verificationCode: String = AppUtil.generate4DigitCode().toString()){

}