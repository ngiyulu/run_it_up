package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.clicksend.SmsService
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.repository.service.OtpDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.OtpResponse
import com.example.runitup.mobile.rest.v1.dto.SendOtpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RequestOtpController:  BaseController<SendOtpRequest, OtpResponse>() {
    @Autowired
    lateinit var otpDbService: OtpDbService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var smsService: SmsService

    override fun execute(request: SendOtpRequest): OtpResponse {
        val user = userRepository.findByPhone(request.phoneNumber)
        val otp = otpDbService.generateOtp(user?.id, request.phoneNumber)
        logger.info("otp object = $otp")
        runBlocking {
            val response = smsService.sendSmsDetailed(otp.phoneNumber, "Your runitup Otp code is ${otp.code}. Do not share it with anyone")
            logger.info("sms response for ${request.phoneNumber} = $response")
        }
        return OtpResponse(true)
    }


}