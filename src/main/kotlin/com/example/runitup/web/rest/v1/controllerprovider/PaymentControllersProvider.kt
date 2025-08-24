package com.example.runitup.web.rest.v1.controllerprovider

import com.example.runitup.web.rest.v1.controllers.payment.CreateCardController
import com.example.runitup.web.rest.v1.controllers.payment.DeleteCardController
import com.example.runitup.web.rest.v1.controllers.payment.UpdateDefaultPaymentController
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