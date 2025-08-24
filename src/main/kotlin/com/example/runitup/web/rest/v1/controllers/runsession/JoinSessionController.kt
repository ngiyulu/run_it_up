package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.enum.PaymentStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Booking
import com.example.runitup.model.BookingPayment
import com.example.runitup.model.RunSession
import com.example.runitup.repository.BookingRepository
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.RunSessionService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.session.JoinSessionModel
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class JoinSessionController: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var sessionService: RunSessionService
    override fun execute(request: com.example.runitup.web.rest.v1.dto.session.JoinSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run = runDb.get()
        // this mean the event is full
        if( user.stripeId == null){
            throw ApiRequestException(text("payment_error"))
        }
        if( !run.isJoinable()){
            throw  ApiRequestException(text("join_error"))
        }
        // this means the run is full, so we return the run to the user
        // and the ui will update, this should only happen if they had an old version of the run
        // that didn't not have the proper ui
        if( run.atFullCapacity()){
            run.updateStatus(user.id.orEmpty())
            return  run
        }
        val availableSpots = run.availableSpots()
        // this means the run is full because he added guests
        if(availableSpots < request.guest){
            run.updateStatus(user.id.orEmpty())
            return  run
        }
        if(run.userHasBookingAlready(user.id.orEmpty())){
            throw ApiRequestException(text("join_invalid"))
        }
        val amount = request.getTotalParticipants() * run.amount
        val runUser = com.example.runitup.web.rest.v1.dto.RunUser(
            auth.name,
            auth.id,
            user.imageUrl,
            0,
            request.guest
        )
        val paymentId = sessionService.joinSession(user.stripeId.orEmpty(), runUser, request.paymentMethodId, amount)
                ?: throw ApiRequestException(text("stripe_error"))
        val booking = bookingRepository.save(Booking(ObjectId().toString(),
            request.getTotalParticipants(),
            user.id.orEmpty(),
            runUser,
            request.sessionId,
            listOf(
                BookingPayment(amount, paymentId)
            ),
            PaymentStatus.PENDING,
            run.amount,
            amount
        ))
        run.bookingList.add(RunSession.SessionRunBooking(
            booking.id.toString(),
            user.id.orEmpty(),
            booking.partySize
            ))
        run.updateTotal()
        return runSessionRepository.save(run)
    }
}