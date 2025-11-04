package com.example.runitup.web.rest.v1.controller

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RefundReasonCode
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.payment.RefundResult
import com.example.runitup.mobile.service.payment.RefundService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateRefundController: BaseController<CreateRefundModel, RefundResult>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository

    @Autowired
    lateinit var refundService: RefundService
    override fun execute(request: CreateRefundModel): RefundResult {
        val bookingDb = bookingRepository.findById(request.bookingId)
        if(!bookingDb.isPresent){
            throw  ApiRequestException("booking_not_found")
        }
        val booking = bookingDb.get()
        val user = cacheManager.getUser(booking.userId) ?: throw ApiRequestException(text("user_not_found"))

        val bookingState = bookingPaymentStateRepository.findByBookingId(booking.id.orEmpty())
            ?: throw  ApiRequestException("booking_error")

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
