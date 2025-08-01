package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.model.User
import com.example.runitup.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetUserController: BaseController<String, User>() {
    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: String): User {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        user.stripeId?.let { it ->
            user.payments = paymentService.listOfCustomerCards(it)?.let { it ->
                it.map {
                    it.mapToUserPayment()
                }
            }
        }
        return  user
    }
}