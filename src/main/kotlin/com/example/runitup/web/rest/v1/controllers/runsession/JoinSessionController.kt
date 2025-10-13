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
import com.example.runitup.service.http.MessagingService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.JoinRunSessionResponse
import com.example.runitup.web.rest.v1.dto.JoinRunSessionStatus
import com.example.runitup.web.rest.v1.dto.session.JoinSessionModel
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateParticipantModel
import model.messaging.Participant
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class JoinSessionController: BaseController<JoinSessionModel, JoinRunSessionResponse>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var sessionService: RunSessionService

    @Autowired
    lateinit var messagingService: MessagingService

    override fun execute(request: com.example.runitup.web.rest.v1.dto.session.JoinSessionModel): JoinRunSessionResponse {
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
            return  JoinRunSessionResponse(JoinRunSessionStatus.FULL, run)
        }
        val availableSpots = run.availableSpots()
        // this means the run is full because he added guests
        if(availableSpots < request.guest){
            return  JoinRunSessionResponse(JoinRunSessionStatus.GUEST_FULL, run)
        }
        if(run.userHasBookingAlready(user.id.orEmpty())){
            return  JoinRunSessionResponse(JoinRunSessionStatus.ALREADY_BOOKED, run)
        }
        val amount = request.getTotalParticipants() * run.amount
        val runUser = com.example.runitup.web.rest.v1.dto.RunUser(
            user.firstName,
            user.lastName,
            user.skillLevel,
            user.id.orEmpty(),
            user.imageUrl,
            0,
            request.guest
        )
        val bookingPayment = mutableListOf<BookingPayment>()
        if(!run.isFree()){
            val paymentId = sessionService.joinSession(user.stripeId.orEmpty(), runUser, request.paymentMethodId, amount)
                ?: throw ApiRequestException(text("stripe_error"))
            bookingPayment.add(BookingPayment(amount, paymentId))
        }

        val booking = bookingRepository.save(Booking(ObjectId().toString(),
            request.getTotalParticipants(),
            user.id.orEmpty(),
            runUser,
            request.sessionId,
            bookingPayment,
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
        val updated =  runSessionRepository.save(run)
        val participant = Participant(
            userId = user.id.orEmpty(),
            role = "member",
            first = user.firstName,
            last = user.lastName,
            joinedAt = user.createdAt,
            lastReadMessageAt = null,
            mutedUntil = null,
            unreadCount = 0
        )
        messagingService.createParticipant(CreateParticipantModel(run.id.orEmpty(), participant)).block()
        return  JoinRunSessionResponse(JoinRunSessionStatus.NONE, updated)
    }
}