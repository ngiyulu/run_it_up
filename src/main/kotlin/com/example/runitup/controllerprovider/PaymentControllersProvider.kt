package com.example.runitup.controllerprovider

import com.example.runitup.controller.payment.CreateCardController
import com.example.runitup.controller.payment.DeleteCardController
import com.example.runitup.controller.payment.UpdateDefaultPaymentController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PaymentControllersProvider {

    @Autowired
    lateinit var createCardController: CreateCardController

    @Autowired
    lateinit var deleteCreateCardController: DeleteCardController

    @Autowired
    lateinit var updateDefaultPaymentController: UpdateDefaultPaymentController
}