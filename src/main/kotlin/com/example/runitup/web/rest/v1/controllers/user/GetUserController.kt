package com.example.runitup.web.rest.v1.controllers.user

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.service.PaymentService
import com.example.runitup.web.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetUserController: BaseController<String, User>() {
    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: String): User {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        user.stripeId?.let { it ->
            user.payments = paymentService.listOfCustomerCards(it)
        }
        return  user
    }
}