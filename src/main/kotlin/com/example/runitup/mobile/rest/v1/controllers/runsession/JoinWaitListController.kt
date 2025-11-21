package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.SetupStatus
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.session.JoinWaitListModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.WaitListPaymentService
import org.bson.types.ObjectId
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class JoinWaitListController: BaseController<JoinWaitListModel, JoinWaitListResponse>() {

    @Autowired
    private lateinit var bookingRepository: BookingRepository


    @Autowired
    lateinit var paymentService: WaitListPaymentService

    @Autowired
    lateinit var runSessionService: RunSessionService


    override fun execute(request: JoinWaitListModel): JoinWaitListResponse {
        val run: RunSession = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user =getMyUser()
        // this mean the event is full
        if (user.stripeId == null) {
            throw ApiRequestException(text("payment_error"))
        }
        if (!run.isJoinable()) {
            throw ApiRequestException(text("join_error"))
        }
        val runUser = RunUser(
            user.firstName,
            user.lastName,
            user.skillLevel,
            user.id.orEmpty(),
            user.imageUrl,
            0,
            guest = 0
        )
        val zonedDateTime = run.date.atStartOfDay(ZoneId.of(run.zoneId))
        val formatter = DateTimeFormatter.ofPattern(AppConstant.DATE_FORMAT)
        val dateString = zonedDateTime.format(formatter)
        val booking = Booking(
            ObjectId().toString(),
            1,
            user.id.orEmpty(),
            runUser,
            request.sessionId,
            PaymentStatus.PENDING,
            run.amount,
            run.amount,
            null,
            null,
            joinedAtFromWaitList = null,
            status = BookingStatus.WAITLISTED,
            customerId = user.stripeId,
            date = dateString
        )
        //user can only join the waitlist if the run is at full capacity
        if (run.atFullCapacity()) {
            if (!run.isSessionFree()) {
                val pm = request.paymentMethodId
                val setupState = paymentService.ensureWaitlistCardReady(
                    bookingId = booking.id.orEmpty(),
                    sessionId = run.id.orEmpty(),
                    userId = booking.userId,
                    customerId = user.stripeId.orEmpty(),
                    paymentMethodId = pm,
                    idempotencyKey = "si-${booking.id}-${pm}"
                )
                if (setupState.status == SetupStatus.REQUIRES_ACTION && setupState.clientSecret != null) {
                    // Return a response to the app that instructs it to present SCA UI with setupState.clientSecret
                    return JoinWaitListResponse(true, setupState.clientSecret, run, true)
                }
                else if(setupState.status  != SetupStatus.SUCCEEDED){
                    throw  ApiRequestException("payment_error")
                }
                booking.setupIntentId = setupState.setupIntentId
                val updatedRun = updateRun(run, booking, runUser)
                runSessionEventLogger.log(
                    sessionId = run.id.orEmpty(),
                    action = RunSessionAction.USER_JOIN_WAITLIST,
                    actor = Actor(ActorType.USER, auth.user.id.orEmpty()),
                    newStatus = null,
                    reason = "Self-join, paid run",
                    correlationId = MDC.get(AppConstant.TRACE_ID),
                    metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
                )
                return JoinWaitListResponse(true, null, updatedRun, false)
            }
            val updatedRun = updateRun(run, booking, runUser)
            runSessionEventLogger.log(
                sessionId = run.id.orEmpty(),
                action = RunSessionAction.USER_JOIN_WAITLIST,
                actor = Actor(ActorType.USER, user.id.orEmpty()),
                newStatus = null,
                reason = "Self-join, free run ",
                correlationId = MDC.get(AppConstant.TRACE_ID),
                metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
            )
            bookingRepository.save(booking)
            // this means the user tried to join the waitlist and the run is free
            return JoinWaitListResponse(true, null, updatedRun, false)

        }
        // tried to join a wailist when the runsession is not ful
        return JoinWaitListResponse(true, null, run, false, refresh = true)
    }

    fun updateRun(run: RunSession, booking:Booking, runUser: RunUser): RunSession{
        run.waitList.add(runUser)
        run.updateTotal()
        val updatedRun = runSessionService.updateRunSession(run)
        bookingRepository.save(booking)
        return  updatedRun
    }
}