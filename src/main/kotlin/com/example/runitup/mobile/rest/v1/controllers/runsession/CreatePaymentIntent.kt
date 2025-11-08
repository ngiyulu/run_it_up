package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.stripe.CreatePIRequest
import com.example.runitup.mobile.rest.v1.dto.stripe.CreatePIResponse
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.PaymentService
import com.example.runitup.mobile.service.myLogger
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
            logger.error("user striped Id = null for user {}", user.id.orEmpty())
            throw ApiRequestException(text("payment_error"))
        }
        request.customerId = user.stripeId
        return paymentService.createPaymentIntent(request) ?: throw ApiRequestException("payment_error")
    }
}