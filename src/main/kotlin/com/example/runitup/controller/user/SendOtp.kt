package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.OtpResponse

import com.example.runitup.repository.service.OtpRepositoryService
import com.example.runitup.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class SendOtp: BaseController<Unit, OtpResponse>() {

    @Autowired
    lateinit var otpRepositoryService: OtpRepositoryService
    override fun execute(request: Unit): OtpResponse {
        val auth =  SecurityContextHolder.getContext().authentication as UserPrincipal
        otpRepositoryService.generateOtp(auth.id.orEmpty(), auth.phoneNumber)
        return OtpResponse(true)
    }


}