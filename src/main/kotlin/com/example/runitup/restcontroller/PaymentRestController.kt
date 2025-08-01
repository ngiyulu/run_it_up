package com.example.runitup.restcontroller


import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/payment")
@RestController
class PaymentRestController {

//    @Autowired
//    lateinit var  paymentControllersProvider: PaymentControllersProvider
//
//    @PostMapping("/create")
//    fun createPayment(@RequestBody model: CreatePaymentModel): PaymentSource {
//        return paymentControllersProvider.createCardController.execute(model)
//    }
//
//    @PostMapping("/delete")
//    fun deletePayment(@RequestBody model: DeleteCardModel): PaymentMethod {
//        return paymentControllersProvider.deleteCreateCardController.execute(model)
//    }
}