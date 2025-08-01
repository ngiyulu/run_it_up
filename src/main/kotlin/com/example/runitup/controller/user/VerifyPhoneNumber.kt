package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.VerifyPhoneNumber
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.repository.service.OtpRepositoryService
import com.example.runitup.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyPhoneNumber: BaseController<VerifyPhoneNumber, User?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var otpRepositoryService: OtpRepositoryService

    override fun execute(request: VerifyPhoneNumber): User? {
        val auth =  SecurityContextHolder.getContext().authentication as UserPrincipal
        val user: User = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("invalid_user"))
        val otp = otpRepositoryService.getOtp(user.id.toString())?: throw ApiRequestException(text("invalid_user"))
        if(otp.code == request.otp){
            otpRepositoryService.disableOtp(otp)
            return  user
        }
        return  null
    }

}