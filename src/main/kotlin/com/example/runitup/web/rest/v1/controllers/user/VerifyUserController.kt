package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.repository.UserRepository
import com.example.runitup.repository.service.OtpDbService
import com.example.runitup.security.JwtTokenService
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import com.example.runitup.utility.AgeUtil
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.VerifyUserFlow
import com.example.runitup.web.rest.v1.dto.VerifyUserRequest
import com.example.runitup.web.rest.v1.dto.VerifyUserResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyUserController: BaseController<Pair<String, VerifyUserRequest>, VerifyUserResponse?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var paymentService: PaymentService


    @Autowired
    lateinit var jwtService: JwtTokenService

    @Autowired
    lateinit var otpDbService: OtpDbService

    override fun execute(request: Pair<String, VerifyUserRequest>): VerifyUserResponse? {
        val  (zoneId, userRequest)= request
        val user = userRepository.findByPhone(userRequest.phone)
        if(user == null){
            // the user has verified and he doesn't have an account
            otpDbService.generateOtp(null, userRequest.phone)
            return VerifyUserResponse(VerifyUserFlow.CREATE,null, null, "", userRequest.phone)
        }

        user.waiverSigned = true
        userRequest.firebaseTokenModel?.let {
            phoneService.createPhone(it)
        }
        val token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
        otpDbService.generateOtp(user.id, userRequest.phone)
        user.stripeId?.let { it ->
            user.payments = paymentService.listOfCustomerCards(it)
        }
        val age = AgeUtil.ageFrom(user.dob, zoneIdString = zoneId)
        if(age >=18 ){
            user.waiverSigned = true
        }
       return VerifyUserResponse(VerifyUserFlow.LOGIN, user, token, user.id.orEmpty(), user.phoneNumber)
    }


}