package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.clicksend.SmsService
import com.example.runitup.mobile.config.AppConfig
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.repository.service.OtpDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.OtpResponse
import com.example.runitup.mobile.rest.v1.dto.SendOtpRequest
import com.example.runitup.mobile.service.EmailService
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

    @Autowired
    lateinit var appConfig: AppConfig

    @Autowired
    lateinit var emailService: EmailService

    override fun execute(request: SendOtpRequest): OtpResponse {
        val user = userRepository.findByPhone(request.phoneNumber)
        val otp = otpDbService.generateOtp(user?.id, request.phoneNumber)
        if(user != null){
            logger.info("otp object = $otp")
            runBlocking {
                if(appConfig.smsEnabled){
                    val response = smsService.sendSmsDetailed(otp.phoneNumber, "Your runitup Otp code is ${otp.code}. Do not share it with anyone")
                    logger.info("sms response for ${user.phoneNumber} = $response")
                }
                else{
                    emailService.sendOtpEmail(user.email, otp.code)
                }
            }
        }

        return OtpResponse(true)
    }


}