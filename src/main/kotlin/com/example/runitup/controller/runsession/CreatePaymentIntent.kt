package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.controller.payment.CreateCardController
import com.example.runitup.dto.stripe.CreatePIRequest
import com.example.runitup.dto.stripe.CreatePIResponse
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreatePaymentIntent: BaseController<CreatePIRequest, CreatePIResponse>()  {

    @Autowired
    lateinit var paymentService: PaymentService
    override fun execute(request: CreatePIRequest): CreatePIResponse {
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        if(user.stripeId == null){
            logger.logError(TAG, "user striped Id = null")
            throw ApiRequestException(text("payment_error"))
        }
        request.customerId = user.stripeId
        return paymentService.createPaymentIntent(request) ?: throw ApiRequestException("payment_error")
    }
}