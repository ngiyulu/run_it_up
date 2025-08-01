package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.VerifyUserRequest
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.model.User
import com.example.runitup.repository.UserRepository
import com.example.runitup.service.PaymentService
import com.example.runitup.service.PhoneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
//login controller
class VerifyUserController: BaseController<VerifyUserRequest, User?>() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var phoneService: PhoneService

    @Autowired
    lateinit var paymentService: PaymentService

    override fun execute(request: VerifyUserRequest): User? {
        val user = userRepository.findByAuth(request.password)
        if(user != null){
            request.tokenModel?.let {
                phoneService.createPhone(it)
            }
            user.stripeId?.let {
                user.payments = paymentService.listOfCustomerCards(it)?.let {
                    it.map {
                        it.mapToUserPayment()
                    }
                }
            }
        }
        return  user
    }


}