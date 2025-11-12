package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.payment.BookingUpdateService
import com.example.runitup.mobile.service.payment.DeltaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UpdateSessionGuest: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var bookingUpdateService: BookingUpdateService

    @Autowired
    lateinit var bookingPricingAdjuster: BookingPricingAdjuster

    @Autowired
    lateinit var bookingStateRepo: BookingPaymentStateRepository


    override fun execute(request: JoinSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run = runSessionService.getRunSession(null, request.sessionId, auth.id) ?: throw ApiRequestException(text("invalid_session_id"))
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
            logger.error("error updating session, user is null {}", request)
            throw ApiRequestException(text("invalid params"))
        }
        val booking = bookingRepository.findByUserIdAndRunSessionIdAndStatusIn(
            signedUpUser.userId,
            request.sessionId,
            mutableListOf(BookingStatus.JOINED)
        ) ?: throw ApiRequestException(text("join_error"))
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

            runSessionEventLogger.log(
                sessionId = run.id.orEmpty(),
                action = RunSessionAction.UPDATE_GUEST,
                actor = Actor(ActorType.USER, user.id.orEmpty()),
                newStatus = null,
                reason = "Guest update new Total of Participants = ${request.getTotalParticipants()} old Total of Participants =${booking.partySize}, session is not free",
                correlationId = MDC.get(AppConstant.TRACE_ID),
                metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
            )
            updateBooking(user, booking, request, newRequestAmount)
            return runSessionService.updateRunSession(run)
        }

        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.UPDATE_GUEST,
            actor = Actor(ActorType.USER, user.id.orEmpty()),
            newStatus = null,
            reason = "Guest update new Total of Participants = ${request.getTotalParticipants()} old Total of Participants =${booking.partySize}, session is free",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        updateBooking(user, booking, request, null)

        return  run
    }

    private fun updateBooking(user: User, booking: Booking, request: JoinSessionModel, newAmount:Double?){
        booking.partySize = request.getTotalParticipants()
        newAmount?.let {
            booking.sessionAmount = it
            booking.currentTotalCents = it.convertToCents()
        }
        bookingRepository.save(booking)
        val map = HashMap<String, String>()
        map[AppConstant.USER_ID] = booking.userId
        map[AppConstant.BOOKING_ID] = booking.id.orEmpty()
        val jobEnvelope = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "${user.getFullName()} updated booking",
            payload = PushJobModel(PushJobType.BOOKING_UPDATED, booking.runSessionId, map)
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_SESSION_PUSH_JOB, jobEnvelope)
        }

    }

}