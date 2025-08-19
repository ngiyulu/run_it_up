package com.example.runitup.restcontroller


import com.example.runitup.controllerprovider.PaymentControllersProvider
import com.example.runitup.dto.CardModel
import com.example.runitup.dto.CreatePaymentModel
import com.example.runitup.dto.DeleteCardModel
import com.stripe.model.PaymentMethod
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
    fun createPayment(@RequestBody model: CreatePaymentModel): CardModel {
        return paymentControllersProvider.createCardController.execute(model)
    }

    @PostMapping("/delete")
    fun deletePayment(@RequestBody model: DeleteCardModel): CardModel {
        return paymentControllersProvider.deleteCreateCardController.execute(model)
    }
}