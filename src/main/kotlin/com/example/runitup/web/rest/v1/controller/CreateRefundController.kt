package com.example.runitup.web.rest.v1.controller

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RefundReasonCode
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.payment.RefundResult
import com.example.runitup.mobile.service.payment.RefundService
import com.example.runitup.web.security.AdminPrincipal
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CreateRefundController: BaseController<CreateRefundModel, RefundResult>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository

    @Autowired
    lateinit var refundService: RefundService
    override fun execute(request: CreateRefundModel): RefundResult {
        val bookingDb = bookingRepository.findById(request.bookingId)
        val auth =  SecurityContextHolder.getContext().authentication
        val savedAdmin = auth.principal as AdminPrincipal
        if(!bookingDb.isPresent){
            throw  ApiRequestException("booking_not_found")
        }
        val booking = bookingDb.get()
        val user = cacheManager.getUser(booking.userId) ?: throw ApiRequestException(text("user_not_found"))

        val bookingState = bookingPaymentStateRepository.findByBookingId(booking.id.orEmpty())
            ?: throw  ApiRequestException("booking_error")
        runSessionEventLogger.log(
            sessionId = request.runSession,
            action = RunSessionAction.USER_KICKED_OUT,
            actor = Actor(ActorType.ADMIN, savedAdmin.admin.id),
            newStatus = null,
            reason = "Admin triggered refund booking = ${request.bookingId}",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        return refundService.refundBooking(
            booking.id.orEmpty(),
            booking.userId,
            user.stripeId.orEmpty(),
            "usd",
            bookingState.totalCapturedCents,
            RefundReasonCode.HOST_CANCELLED,
        )
    }
}

data class CreateRefundModel(val bookingId:String, val runSession:String)
