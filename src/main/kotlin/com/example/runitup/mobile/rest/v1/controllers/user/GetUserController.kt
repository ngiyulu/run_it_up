package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.http.MessagingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetUserController: BaseController<String, User>() {
    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var messagingService: MessagingService
    override fun execute(request: String): User {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        user.stripeId?.let { it ->
            user.payments = paymentService.listOfCustomerCards(it)
        }
        return user



    }
}