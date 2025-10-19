package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.enum.SkillLevel
import com.example.runitup.mobile.utility.AppUtil

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