package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.OtpResponse
import com.example.runitup.dto.SendOtpRequest
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Otp

import com.example.runitup.repository.service.OtpRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetOtpController:  BaseController<String, Otp>() {

    @Autowired
    lateinit var otpRepositoryService: OtpRepositoryService
    override fun execute(request: String): Otp {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("invalid_user"))
        return otpRepositoryService.getOtp(user.id.orEmpty())?: throw ApiRequestException("error")
    }
}