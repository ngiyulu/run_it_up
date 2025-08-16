package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.OtpResponse
import com.example.runitup.dto.SendOtpRequest
import com.example.runitup.exception.ApiRequestException

import com.example.runitup.repository.service.OtpRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SendOtpController:  BaseController<SendOtpRequest, OtpResponse>() {

    @Autowired
    lateinit var otpRepositoryService: OtpRepositoryService
    override fun execute(request: SendOtpRequest): OtpResponse {
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException(text("invalid_user"))
        print(user)
        val otp = otpRepositoryService.generateOtp(user.id.orEmpty(), user.phoneNumber)
        print(otp)
        return OtpResponse(true)
    }


}