package com.example.runitup.controller.payment

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.DeleteCardModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.service.PaymentService
import com.stripe.model.PaymentMethod
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DeleteCardController: BaseController<DeleteCardModel, PaymentMethod>() {

    @Autowired
    lateinit var paymentService: PaymentService

    override fun execute(request: DeleteCardModel): PaymentMethod {
        if (request.paymentId.isEmpty()) {
            throw ApiRequestException(text("invalid_params"))
        }
        return paymentService.deleteCard(request.paymentId)
            ?: throw ApiRequestException(text("payment_error"))
    }
}

