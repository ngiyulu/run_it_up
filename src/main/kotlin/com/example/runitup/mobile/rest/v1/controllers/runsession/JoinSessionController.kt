package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.JoinRunSessionResponse
import com.example.runitup.mobile.rest.v1.dto.JoinRunSessionStatus
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.http.MessagingService
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

    override fun execute(request: JoinSessionModel): JoinRunSessionResponse {
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
        val runUser = RunUser(
            user.firstName,
            user.lastName,
            user.skillLevel,
            user.id.orEmpty(),
            user.imageUrl,
            0,
            request.guest
        )
        val bookingPayment = mutableListOf<com.example.runitup.mobile.model.BookingPayment>()
        if(!run.isFree()){
            val paymentId = sessionService.joinSession(user.stripeId.orEmpty(), runUser, request.paymentMethodId.orEmpty(), amount)
                ?: throw ApiRequestException(text("stripe_error"))
            bookingPayment.add(com.example.runitup.mobile.model.BookingPayment(amount, paymentId))
        }

        val booking = bookingRepository.save(
            com.example.runitup.mobile.model.Booking(
                ObjectId().toString(),
                request.getTotalParticipants(),
                user.id.orEmpty(),
                runUser,
                request.sessionId,
                bookingPayment,
                PaymentStatus.PENDING,
                run.amount,
                amount
            )
        )
        run.bookingList.add(
            com.example.runitup.mobile.model.RunSession.SessionRunBooking(
            booking.id.toString(),
            user.id.orEmpty(),
            booking.partySize
            ))
        run.updateTotal()
        val updated =  runSessionRepository.save(run)
        val participant = Participant(
            userId = user.id.orEmpty(),
            role = "MEMBER",
            first = user.firstName,
            last = user.lastName,
            joinedAt = user.createdAt.toEpochDay(),
            lastReadMessageAt = null,
            mutedUntil = null,
            unreadCount = 0
        )
        messagingService.createParticipant(CreateParticipantModel(run.id.orEmpty(), participant, run.getConversationTitle())).block()
        return  JoinRunSessionResponse(JoinRunSessionStatus.NONE, updated)
    }
}