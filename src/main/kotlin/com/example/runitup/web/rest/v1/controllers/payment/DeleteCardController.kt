package com.example.runitup.web.rest.v1.controllers.payment

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.extensions.mapToUserPayment
import com.example.runitup.service.PaymentService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.payment.CardModel
import com.example.runitup.web.rest.v1.dto.payment.DeleteCardModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DeleteCardController: BaseController<DeleteCardModel, CardModel>() {

    @Autowired
    lateinit var paymentService: PaymentService

    override fun execute(request: com.example.runitup.web.rest.v1.dto.payment.DeleteCardModel): com.example.runitup.web.rest.v1.dto.payment.CardModel {
        if (request.paymentId.isEmpty()) {
            throw ApiRequestException(text("invalid_params"))
        }
        val payment = paymentService.deleteCard(request.paymentId) ?: throw ApiRequestException(text("payment_error"))

        return  payment.mapToUserPayment(false)

    }
}

