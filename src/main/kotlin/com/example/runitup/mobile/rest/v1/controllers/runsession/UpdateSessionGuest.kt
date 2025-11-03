package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.payment.BookingUpdateService
import com.example.runitup.mobile.service.payment.DeltaType
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateSessionGuest: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository


    @Autowired
    private lateinit var bookingRepository: BookingRepository


    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var bookingUpdateService: BookingUpdateService

    @Autowired
    lateinit var bookingPricingAdjuster: BookingPricingAdjuster

    @Autowired
    lateinit var bookingStateRepo: BookingPaymentStateRepository


    override fun execute(request: JoinSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run =
            runSessionService.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        if (run.atFullCapacity()) {
            //TODO: later we can find a way to add the guest to the waitlist
            throw ApiRequestException(text("full_capacity"))
        }
        if (!run.isJoinable()) {
            throw ApiRequestException(text("join_error"))
        }
        // we find the user making the update
        val signedUpUser = run.bookingList.find { it.userId == user.id }
        // this means the user making the call is not part of the session
        if (signedUpUser == null) {
            logger.logError("error updating session, user is null", null)
            throw ApiRequestException(text("invalid params"))
        }
        val booking = bookingRepository.findByUserIdAndRunSessionIdAndStatusIn(
            signedUpUser.userId,
            request.sessionId,
            mutableListOf(BookingStatus.JOINED)
        )
            ?: throw ApiRequestException(text("join_error"))
        val type = bookingUpdateService.deltaChange(auth.id.orEmpty(), request, booking)

        println("type = $type")
        if (type == DeltaType.SAME) {
            return run
        }
        // this means the user is adding more people
        if (type == DeltaType.INCREASED) {
            // we have to check to make sure the run is not at full capacity
            if (run.atFullCapacity()) {
                throw ApiRequestException(text("full_capacity"))
            }
        }
        if(!run.isSessionFree()){
            val newRequestAmount = request.getTotalParticipants() * run.amount
            val amountInCents = newRequestAmount.convertToCents()
            // get the booking state that has information about the amount
            val bookingState = bookingStateRepo.findByBookingId(booking.id.orEmpty()) ?: throw ApiRequestException(text("error"))
            if (type == DeltaType.INCREASED) {
                val model = bookingPricingAdjuster.createDeltaHoldForIncrease(
                    booking.id.orEmpty(),
                    user.id.orEmpty(),
                    user.stripeId.orEmpty(),
                    "usd",
                    bookingState.totalAuthorizedCents,
                    amountInCents,
                    booking.getNumOfGuest(),
                    request.guest,
                    booking.paymentMethodId.orEmpty(),
                    user.id.orEmpty()
                )
                if(!model.ok){
                    throw ApiRequestException(model.message.orEmpty())
                }
            }
            else{
                // decrease
                val decreaseModel = bookingPricingAdjuster.handleDecreaseBeforeCapture(
                    booking.id.orEmpty(),
                    user.id.orEmpty(),
                    user.stripeId.orEmpty(),
                    "usd",
                    bookingState.totalAuthorizedCents,
                    amountInCents,
                    booking.paymentId,
                    booking.paymentMethodId,
                    user.id.orEmpty()
                )
                if(!decreaseModel.ok){
                    throw ApiRequestException(decreaseModel.message.orEmpty())
                }
            }

            updateBooking(booking, request, newRequestAmount)
            return runSessionService.updateRunSession(run)
        }
        updateBooking(booking, request, null)
        return  run
    }

    private fun updateBooking(booking: Booking, request: JoinSessionModel, newAmount:Double?){
        booking.partySize = request.getTotalParticipants()
        newAmount?.let {
            booking.sessionAmount = it
            booking.currentTotalCents = it.convertToCents()
        }
        bookingRepository.save(booking)
    }





}