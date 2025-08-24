package com.example.runitup.web.rest.v1.controllers

import com.example.runitup.model.User
import com.example.runitup.repository.GymRepository
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.JwtTokenService
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import com.example.runitup.web.rest.v1.dto.initialize.InitializeRequest
import com.example.runitup.web.rest.v1.dto.initialize.InitializeResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class InitializeController: BaseController<InitializeRequest, InitializeResponse>() {

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var jwtService: JwtTokenService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: com.example.runitup.web.rest.v1.dto.initialize.InitializeRequest): com.example.runitup.web.rest.v1.dto.initialize.InitializeResponse {
        val gyms = gymRepository.findAll()
        var user: User? = null
        var token:String? = null
        if(request.userId != null){
            val dbRes = userRepository.findById(request.userId)
            if(dbRes.isPresent){
                user = dbRes.get()
                user.stripeId?.let { it ->
                    user.payments = paymentService.listOfCustomerCards(it)
                }
                token = jwtService.generateToken(UserPrincipal(user.id.toString(), user.email, user.getFullName(), user.phoneNumber, user.auth))
            }
            request.firebaseTokenModel?.let {
                phoneService.createPhone(it)
            }
        }
        return com.example.runitup.web.rest.v1.dto.initialize.InitializeResponse(gyms, user, token.orEmpty(), true, 3)
    }
}