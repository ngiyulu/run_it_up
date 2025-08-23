package com.example.runitup.dto

import com.example.runitup.enum.PaymentStatus
import com.example.runitup.model.RunSession
import com.example.runitup.utility.AppUtil
import java.time.LocalDateTime

class RunUser(
    val name: String,
    val userId: String?,
    var imageUrl:String?,
    var checkIn: Int = 0,
    var guest: Int,
    var verificationCode: String = AppUtil.generate4DigitCode().toString()){

}