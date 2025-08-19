package com.example.runitup.controller.payment

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.CardModel
import com.example.runitup.dto.CreatePaymentModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateCardController: BaseController<CreatePaymentModel, CardModel>() {

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: CreatePaymentModel): CardModel {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if (user.stripeId == null) {
            logger.logError(CreateCardController::class.java.name, "user striped Id = null")
            throw ApiRequestException(text("payment_error"))
        }
        if (request.token.isEmpty()) {
            throw ApiRequestException(text("invalid_token"))
        }
        val payment = paymentService.createCard(user.stripeId.orEmpty(), request.token)
            ?: throw ApiRequestException(text("payment_error"))

        return   CardModel(
            id = payment.id,
            brand = payment.card.brand,
            last4 = payment.card.last4,
            expMonth = payment.card.expMonth.toInt(),
            expYear = payment.card.expYear.toInt()
        )

    }
}