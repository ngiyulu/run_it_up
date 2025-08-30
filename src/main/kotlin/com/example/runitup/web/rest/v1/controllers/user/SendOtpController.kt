package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.service.OtpService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.OtpResponse
import com.example.runitup.web.rest.v1.dto.SendOtpRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SendOtpController:  BaseController<SendOtpRequest, OtpResponse>() {

    @Autowired
    lateinit var otpService: OtpService
    override fun execute(request: SendOtpRequest): OtpResponse {
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException(text("invalid_user"))
        val otp = otpService.createOtp(user)
        print(otp)
        return OtpResponse(true)
    }


}