package com.example.runitup.mobile.rest.v1.controllers.payment

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.example.runitup.mobile.rest.v1.dto.payment.CreatePaymentModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.myLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateCardController: BaseController<CreatePaymentModel, List<CardModel>?>() {

    @Autowired
    lateinit var paymentService: PaymentService

    private val log = myLogger()

    override fun execute(request: CreatePaymentModel): List<CardModel>? {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if (user.stripeId == null) {
            log.error("user striped Id = null for user {}", user.id.orEmpty())
            throw ApiRequestException(text("payment_error"))
        }
        if (request.paymentMethod.isEmpty()) {
            throw ApiRequestException(text("invalid_token"))
        }
        val payment = paymentService.createPaymentMethod(user.stripeId.orEmpty(), request.paymentMethod)
        payment.error?.let {
            throw ApiRequestException(it.message.orEmpty())
        }
        return   paymentService.listOfCustomerCards(user.stripeId.orEmpty())
    }
}