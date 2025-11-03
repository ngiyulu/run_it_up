package com.example.runitup.mobile.rest.v1.restcontroller


import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllerprovider.PaymentControllersProvider
import com.example.runitup.mobile.rest.v1.dto.payment.CardModel
import com.example.runitup.mobile.rest.v1.dto.payment.CreatePaymentModel
import com.example.runitup.mobile.rest.v1.dto.payment.DeleteCardModel
import com.example.runitup.mobile.rest.v1.dto.payment.UpdateDefaultCardModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/payment")
@RestController
class PaymentRestController {

    @Autowired
    lateinit var  paymentControllersProvider: PaymentControllersProvider

    @PostMapping("/create")
    fun createPayment(@RequestBody model: CreatePaymentModel): List<CardModel>? {
        return paymentControllersProvider.createCardController.execute(model)
    }

    @PostMapping("/update/default-payment")
    fun updateDefaultPayment(@RequestBody model: UpdateDefaultCardModel): User {
        return paymentControllersProvider.updateDefaultPaymentController.execute(model)
    }

    @PostMapping("/delete")
    fun deletePayment(@RequestBody model: DeleteCardModel): CardModel {
        return paymentControllersProvider.deleteCreateCardController.execute(model)
    }
}