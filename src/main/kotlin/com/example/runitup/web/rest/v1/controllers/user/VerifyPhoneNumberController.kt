package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.repository.service.OtpDbService
import com.example.runitup.security.JwtTokenService
import com.example.runitup.security.UserPrincipal
import com.example.runitup.utility.AgeUtil
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.VerifyPhoneNumberRequest
import com.example.runitup.web.rest.v1.dto.VerifyPhoneNumberResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyPhoneNumberController: BaseController<Pair<String, VerifyPhoneNumberRequest>, VerifyPhoneNumberResponse?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var otpDbService: OtpDbService

    @Autowired
    lateinit var jwtService: JwtTokenService

    override fun execute(request: Pair<String, VerifyPhoneNumberRequest>): VerifyPhoneNumberResponse {
        val (zoneId, userRequest) = request
        val otp = otpDbService.getOtp(userRequest.phoneNumber)?: throw ApiRequestException(text("error"))
        print(otp)
        if(otp.code == userRequest.otp){
            // this means the use has to create a new account

            if(otp.userId == null){
                return VerifyPhoneNumberResponse(true, null, null)
            }
            // this means user already has an account
            val user: User = cacheManager.getUser(otp.userId.orEmpty()) ?: throw ApiRequestException(text("invalid_user"))
            val token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            otpDbService.disableOtp(otp)
            val age = AgeUtil.ageFrom(user.dob, zoneIdString = zoneId)
            println("age = $age")
            if(!user.waiverSigned){
                user.waiverSigned = age >= 18
            }
            return VerifyPhoneNumberResponse(true, user, token)
        }
        return VerifyPhoneNumberResponse(false, null, null)
    }

}