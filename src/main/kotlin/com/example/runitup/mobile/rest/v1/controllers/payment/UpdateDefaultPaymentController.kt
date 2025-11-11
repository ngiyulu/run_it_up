package com.example.runitup.mobile.rest.v1.controllers.payment

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.payment.UpdateDefaultCardModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateDefaultPaymentController: BaseController<UpdateDefaultCardModel, User>() {

    @Autowired
    lateinit var paymentService: PaymentService

    override fun execute(request: UpdateDefaultCardModel): User {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if (user.stripeId == null) {
            logger.error("user striped Id = null for user {}", user.id.orEmpty())
            throw ApiRequestException(text("payment_error"))
        }
        if(request.paymentId == AppConstant.WALLET){
            user.defaultPayment = AppConstant.WALLET
        }
        else{
            user.defaultPayment = request.paymentId
            val model = paymentService.makeDefaultCard(user.stripeId.orEmpty(), request.paymentId)
            if(!model.ok){
                throw ApiRequestException(model.error.orEmpty())
            }
        }

        return cacheManager.updateUser(user)
    }
}