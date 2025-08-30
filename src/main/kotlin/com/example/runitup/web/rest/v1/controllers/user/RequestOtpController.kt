package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.repository.service.OtpDbService
import com.example.runitup.service.OtpService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.OtpResponse
import com.example.runitup.web.rest.v1.dto.SendOtpRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RequestOtpController:  BaseController<SendOtpRequest, OtpResponse>() {
    @Autowired
    lateinit var otpDbService: OtpDbService
    override fun execute(request: SendOtpRequest): OtpResponse {
        val otp = otpDbService.generateOtp(request.phoneNumber)
        print(otp)
        return OtpResponse(true)
    }


}