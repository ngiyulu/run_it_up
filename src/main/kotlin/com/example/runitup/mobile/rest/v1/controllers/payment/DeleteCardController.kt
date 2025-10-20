package com.example.runitup.mobile.rest.v1.controllers.payment

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.mapToUserPayment
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.example.runitup.mobile.rest.v1.dto.payment.DeleteCardModel
import com.example.runitup.mobile.service.PaymentService
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

