package com.example.runitup.web.rest.v1.dto

import com.example.runitup.utility.AppUtil

class RunUser(
    val name: String,
    val userId: String?,
    var imageUrl:String?,
    var checkIn: Int = 0,
    var guest: Int,
    var verificationCode: String = AppUtil.generate4DigitCode().toString()){

}