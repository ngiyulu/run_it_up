package com.example.runitup.mobile.rest.v1.controllers.waitlist


import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FirebaseTokenModel
import com.example.runitup.mobile.rest.v1.restcontroller.WaitlistRefreshRequest
import com.example.runitup.mobile.rest.v1.restcontroller.WaitlistRefreshResponse
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.WaitListPaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class RefreshWaitListController: BaseController<WaitlistRefreshRequest, WaitlistRefreshResponse>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var waitListPaymentService: WaitListPaymentService
    override fun execute(request: WaitlistRefreshRequest): WaitlistRefreshResponse {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val bookingDb = bookingRepository.findById(request.bookingId)
        if(!bookingDb.isPresent){
            throw ApiRequestException("booking_error")
        }
        val booking = bookingDb.get()
        val state = waitListPaymentService.refreshWaitlistSetupState(request.setupIntentId)
            ?: throw  ApiRequestException("payment_error")
        val canAutoCharge = state.status.name == "SUCCEEDED" && booking.status == BookingStatus.WAITLISTED
        return WaitlistRefreshResponse(
            ok = true,
            status = state.status.name,
            setupIntentId = request.setupIntentId,
            needsUserAction = state.needsUserAction,
            message = if (state.needsUserAction) "Further action required" else "Ready for off-session",
            canAutoCharge = canAutoCharge
        )

    }

    class CreateOrUpdatePhoneModel(val token: FirebaseTokenModel, val phoneOS:String)
}