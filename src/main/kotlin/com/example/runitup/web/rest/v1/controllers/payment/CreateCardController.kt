package com.example.runitup.web.rest.v1.controllers.payment

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.payment.CardModel
import com.example.runitup.web.rest.v1.dto.payment.CreatePaymentModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateCardController: BaseController<CreatePaymentModel, List<CardModel>?>() {

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: com.example.runitup.web.rest.v1.dto.payment.CreatePaymentModel): List<com.example.runitup.web.rest.v1.dto.payment.CardModel>? {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if (user.stripeId == null) {
            logger.logError(CreateCardController::class.java.name, "user striped Id = null")
            throw ApiRequestException(text("payment_error"))
        }
        if (request.paymentMethod.isEmpty()) {
            throw ApiRequestException(text("invalid_token"))
        }
        paymentService.createPaymentMethod(user.stripeId.orEmpty(), request.paymentMethod)
            ?: throw ApiRequestException(text("payment_error"))

        return   paymentService.listOfCustomerCards(user.stripeId.orEmpty())

    }
}