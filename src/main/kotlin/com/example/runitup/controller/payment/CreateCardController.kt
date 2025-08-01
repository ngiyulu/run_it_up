package com.example.runitup.controller.payment

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.CreatePaymentModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.stripe.model.PaymentSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateCardController: BaseController<CreatePaymentModel, PaymentSource>() {

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: CreatePaymentModel): PaymentSource {
        val auth = SecurityContextHolder.getContext().authentication as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if (user.stripeId == null) {
            logger.logError(CreateCardController::class.java.name, "user striped Id = null")
            throw ApiRequestException(text("payment_error"))
        }
        if (request.token.isEmpty()) {
            throw ApiRequestException(text("invalid_token"))
        }
        return paymentService.createCard(user.stripeId.orEmpty(), request.token)
            ?: throw ApiRequestException(text("payment_error"))
    }
}