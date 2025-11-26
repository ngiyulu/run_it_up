package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.repository.service.OtpDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.OtpResponse
import com.example.runitup.mobile.rest.v1.dto.SendOtpRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RequestOtpController:  BaseController<SendOtpRequest, OtpResponse>() {
    @Autowired
    lateinit var otpDbService: OtpDbService

    @Autowired
    lateinit var userRepository: UserRepository

    override fun execute(request: SendOtpRequest): OtpResponse {
        val user = userRepository.findByPhone(request.phoneNumber)
        val otp = otpDbService.generateOtp(user?.id, request.phoneNumber)
        print(otp)
        return OtpResponse(true)
    }


}