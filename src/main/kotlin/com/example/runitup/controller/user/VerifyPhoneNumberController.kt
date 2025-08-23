package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.VerifyPhoneNumberRequest
import com.example.runitup.dto.VerifyPhoneNumberResponse
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.repository.service.OtpDbService
import com.example.runitup.security.JwtTokenService
import com.example.runitup.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyPhoneNumberController: BaseController<VerifyPhoneNumberRequest, VerifyPhoneNumberResponse?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var otpDbService: OtpDbService

    @Autowired
    lateinit var jwtService: JwtTokenService

    override fun execute(request: VerifyPhoneNumberRequest): VerifyPhoneNumberResponse{
        val user: User = cacheManager.getUser(request.userId) ?: throw ApiRequestException(text("invalid_user"))
        val otp = otpDbService.getOtp(user.id.toString())?: throw ApiRequestException(text("error"))
        print(otp)
        if(otp.code == request.otp){
            val token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            otpDbService.disableOtp(otp)
            return  VerifyPhoneNumberResponse(true, user, token)
        }
        return  VerifyPhoneNumberResponse(false, null, null)
    }

}