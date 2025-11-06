package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.JoinRunSessionResponse
import com.example.runitup.mobile.rest.v1.dto.JoinRunSessionStatus
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateParticipantModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import model.messaging.Participant
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class JoinSessionController: BaseController<JoinSessionModel, JoinRunSessionResponse>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var sessionService: RunSessionService

    @Autowired
    lateinit var bookingPricingAdjuster: BookingPricingAdjuster

    @Autowired
    lateinit var messagingService: MessagingService

    @Autowired
    lateinit var pushNotificationService: RunSessionPushNotificationService

    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var appScope: CoroutineScope

    override fun execute(request: JoinSessionModel): JoinRunSessionResponse {
        val runDb = runSessionRepository.findById(request.sessionId)
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run = runDb.get()
        // this mean the event is full
        if( user.stripeId == null || (run.isFree && request.paymentMethodId == null)){
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
        val booking = Booking(
            ObjectId().toString(),
            request.getTotalParticipants(),
            user.id.orEmpty(),
            runUser,
            request.sessionId,
            PaymentStatus.PENDING,
            run.amount,
            amount,
            null,
            null,
            null,
            0,
            joinedAtFromWaitList = null,
            currentTotalCents = amount.convertToCents(),
            customerId = user.stripeId
        )
        if(!run.isSessionFree()){
            val holdingCharge = bookingPricingAdjuster.createPrimaryHoldWithChange(
                booking.id.orEmpty(),
                user.id.orEmpty(),
                user.stripeId.toString(),
                "usd",
                amount.convertToCents(),
                request.paymentMethodId.orEmpty(),
                user.id.orEmpty(),
            )
            if(!holdingCharge.ok){
                throw  ApiRequestException(holdingCharge.message.toString())
            }
            booking.paymentId = holdingCharge.paymentIntentId
            booking.paymentMethodId = request.paymentMethodId
        }
        bookingRepository.save(booking)
        run.bookingList.add(
            RunSession.SessionRunBooking(
                booking.id.toString(),
                user.id.orEmpty(),
                booking.partySize
            )
        )
        run.updateTotal()
        val updated =  sessionService.updateRunSession(run)
        val participant = Participant(
            userId = user.id.orEmpty(),
            role = "MEMBER",
            first = user.firstName,
            last = user.lastName,
            joinedAt = user.createdAt.epochSecond,
            lastReadMessageAt = null,
            mutedUntil = null,
            unreadCount = 0
        )
        val data = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Joined run",
            payload = JoinSessionQueueModel(user.id.toString(), run.id.orEmpty()),
        )
        appScope.launch {
            // this event is to send a text message to user to let them know they have joined
            // the run successfully
            // if the session is already confirmed, we don't need to confirm again
            if(run.status == RunStatus.PENDING){
                queueService.sendJob(QueueNames.JOINED_RUN_JOB, data)
            }
            // we have to trigger this event to confirm session if the number of participants have been reached
            queueService.sendJob(QueueNames.RUN_CONFIRMATION_JOB, data)
        }
        run.hostedBy?.let {
            pushNotificationService.userJoinedRunSession(it, user, run)
        }
        messagingService.createParticipant(CreateParticipantModel(run.id.orEmpty(), participant, run.getConversationTitle())).block()
        return  JoinRunSessionResponse(JoinRunSessionStatus.NONE, updated)
    }
}

class JoinSessionQueueModel(val userId:String, val runSessionId:String)