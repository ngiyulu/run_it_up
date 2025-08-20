package com.example.runitup.controller.payment

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.CardModel
import com.example.runitup.dto.DeleteCardModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.service.PaymentService
import com.stripe.model.PaymentMethod
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DeleteCardController: BaseController<DeleteCardModel, CardModel>() {

    @Autowired
    lateinit var paymentService: PaymentService

    override fun execute(request: DeleteCardModel): CardModel {
        if (request.paymentId.isEmpty()) {
            throw ApiRequestException(text("invalid_params"))
        }
        val payment = paymentService.deleteCard(request.paymentId) ?: throw ApiRequestException(text("payment_error"))

        return  payment.mapToUserPayment(false)

    }
}

