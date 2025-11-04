package com.example.runitup.mobile.rest.v1.dto

import com.example.runitup.mobile.enum.SkillLevel
import com.example.runitup.mobile.utility.AppUtil

class RunUser(
    val first: String = "",
    val last: String = "",
    var level: SkillLevel? = null,
    val userId: String? = null,
    var imageUrl:String? = null,
    var checkIn: Int = 0,
    var guest: Int = 0,
    var verificationCode: String = AppUtil.generate4DigitCode().toString()){

}