package com.example.runitup.web.rest.v1.controllers.payment

import com.example.runitup.constants.AppConstant
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.payment.UpdateDefaultCardModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateDefaultPaymentController: BaseController<UpdateDefaultCardModel, User>() {

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: com.example.runitup.web.rest.v1.dto.payment.UpdateDefaultCardModel): User {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if (user.stripeId == null) {
            logger.logError(CreateCardController::class.java.name, "user striped Id = null")
            throw ApiRequestException(text("payment_error"))
        }
        if(request.paymentId == AppConstant.WALLET){
            user.defaultPayment = AppConstant.WALLET
        }
        else{
            user.defaultPayment = request.paymentId
            paymentService.makeDefaultCard(user.stripeId.orEmpty(), request.paymentId)
                ?: throw ApiRequestException(text("payment_error"))
        }

        return cacheManager.updateUser(user)
    }
}