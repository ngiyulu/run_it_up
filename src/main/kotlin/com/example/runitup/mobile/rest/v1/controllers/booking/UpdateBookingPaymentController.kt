package com.example.runitup.mobile.rest.v1.controllers.booking

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.constants.AppConstant.BOOKING_ID
import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.getAllBookingStatuses
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.service.BookingService
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UpdateBookingPaymentController: BaseController<UpdateBookingPaymentModel, BookingDetails>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var bookingService: BookingService
    override fun execute(request: UpdateBookingPaymentModel): BookingDetails {
        val user =getMyUser()
        val bookingDb = bookingRepository.findById(request.bookingId)
        if(!bookingDb.isPresent){
            throw  ApiRequestException("booking_not_found")
        }
        val booking = bookingDb.get()
        if(!booking.isPaymentStatusManualUpdateAllowed()){
            throw  ApiRequestException("error")
        }
        val run = cacheManager.getRunSession(booking.runSessionId) ?: throw  ApiRequestException("run_not_found")
        val status = run.status
        if(status == RunStatus.CANCELLED || status  == RunStatus.CONFIRMED || status == RunStatus.PROCESSED){
            throw  ApiRequestException("invalid_request")
        }
        booking.paymentStatus = PaymentStatus.MANUAL_PAID
        booking.paidAt = Instant.now()
        bookingRepository.save(booking)
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.BOOKING_PAID_MANUALLY,
            actor = Actor(ActorType.USER, user.id.orEmpty()),
            newStatus = null,
            reason = "Admin manually marked booking as paid",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(
                AppConstant.SOURCE to MDC.get(AppConstant.SOURCE),
                BOOKING_ID to booking.id.orEmpty()
                )
        )
        return bookingService.getBookingDetails(run)
    }

}


data class UpdateBookingPaymentModel(val bookingId:String)