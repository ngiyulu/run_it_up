package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.VerifyUserRequest
import com.example.runitup.dto.VerifyUserResponse
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.JwtService
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyUserController: BaseController<VerifyUserRequest, VerifyUserResponse?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var jwtService: JwtService

    override fun execute(request: VerifyUserRequest): VerifyUserResponse? {
        val user = userRepository.findByPhone(request.phone)
        var response: VerifyUserResponse? = null
        if(user != null){
            request.tokenModel?.let {
                phoneService.createPhone(it)
            }
            user.stripeId?.let { it ->
                user.payments = paymentService.listOfCustomerCards(it)?.let { it ->
                    it.map {
                        it.mapToUserPayment()
                    }
                }
            }
            val token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            response = VerifyUserResponse(user, token)

        }
        return  response
    }


}