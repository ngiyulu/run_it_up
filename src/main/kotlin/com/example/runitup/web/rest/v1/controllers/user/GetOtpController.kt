package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Otp
import com.example.runitup.repository.service.OtpDbService
import com.example.runitup.web.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetOtpController:  BaseController<String, Otp>() {

    @Autowired
    lateinit var otpDbService: OtpDbService
    override fun execute(request: String): Otp {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("invalid_user"))
        return otpDbService.getOtp(user.id.orEmpty())?: throw ApiRequestException("error")
    }
}