package com.example.runitup.web.rest.v1.restcontroller


import com.example.runitup.model.User
import com.example.runitup.web.rest.v1.controllerprovider.PaymentControllersProvider
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
    fun createPayment(@RequestBody model: com.example.runitup.web.rest.v1.dto.payment.CreatePaymentModel): List<com.example.runitup.web.rest.v1.dto.payment.CardModel>? {
        return paymentControllersProvider.createCardController.execute(model)
    }

    @PostMapping("/update/default-payment")
    fun updateDefaultPayment(@RequestBody model: com.example.runitup.web.rest.v1.dto.payment.UpdateDefaultCardModel): User {
        return paymentControllersProvider.updateDefaultPaymentController.execute(model)
    }

    @PostMapping("/delete")
    fun deletePayment(@RequestBody model: com.example.runitup.web.rest.v1.dto.payment.DeleteCardModel): com.example.runitup.web.rest.v1.dto.payment.CardModel {
        return paymentControllersProvider.deleteCreateCardController.execute(model)
    }
}