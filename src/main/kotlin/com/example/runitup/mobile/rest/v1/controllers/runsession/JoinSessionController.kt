package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.mobile.rest.v1.dto.session.JoinWaitListModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.NumberGenerator
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateParticipantModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import model.messaging.Participant
import org.bson.types.ObjectId
import org.jboss.logging.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class JoinSessionController: BaseController<JoinSessionModel, JoinRunSessionResponse>() {


    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @Autowired
    private lateinit var sessionService: RunSessionService

    @Autowired
    private lateinit var joinWaitListController: JoinWaitListController

    @Autowired
    lateinit var bookingPricingAdjuster: BookingPricingAdjuster

    @Autowired
    lateinit var messagingService: MessagingService

    @Autowired
    lateinit var numberGenerator: NumberGenerator

    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var appScope: CoroutineScope

    override fun execute(request: JoinSessionModel): JoinRunSessionResponse {
        val run = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        val user =getMyUser()
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
        // when waitlist is not empty but run is not at capacity, it means a user jus left but the waitlist promotion job hasn't ran yet
        // so we need to add user to waitlist and let the job take care of the rest
        if(run.atFullCapacity() || run.waitList.isNotEmpty()){
            // join waitlist and let the job take care of promoting the user
            logger.info("we are waitlisting user")
            joinWaitListController.execute(JoinWaitListModel(request.sessionId, "Join session API", request.paymentMethodId.orEmpty()))
            return  JoinRunSessionResponse(JoinRunSessionStatus.WAITLISTED, run)
        }
        val availableSpots = run.availableSpots()
        // this means the run is full because he added guests
        if(availableSpots < request.guest){
            return  JoinRunSessionResponse(JoinRunSessionStatus.GUEST_FULL, run)
        }
        if(run.userHasBookingAlready(user.id.orEmpty())){
            return  JoinRunSessionResponse(JoinRunSessionStatus.ALREADY_BOOKED, run)
        }
        if(run.privateRun && run.code != null ){
            if(!numberGenerator.validateEncryptedCode(run.code!!, request.code)){
                throw ApiRequestException(text("invalid_code"))
            }
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
        val zonedDateTime = run.date.atStartOfDay(ZoneId.of(run.zoneId))
        val formatter = DateTimeFormatter.ofPattern(AppConstant.DATE_FORMAT)
        val dateString = zonedDateTime.format(formatter)
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
            customerId = user.stripeId,
            date = dateString
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


        updateQueue(run, user.id.orEmpty(), booking.id.orEmpty())
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.USER_JOINED,
            actor = Actor(ActorType.USER, user.id.orEmpty()),
            newStatus = null,
            reason = "Self-join",
            correlationId = MDC.get(AppConstant.TRACE_ID) as? String,
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        messagingService.createParticipant(CreateParticipantModel(run.id.orEmpty(), participant, run.getConversationTitle())).block()
        return  JoinRunSessionResponse(JoinRunSessionStatus.NONE, updated)
    }


    fun updateQueue(runSession: RunSession, userId: String, bookingId:String){
        val map = HashMap<String, String>()
        map[AppConstant.USER_ID] = userId
        map[AppConstant.BOOKING_ID] = bookingId
        val jobEnvelope = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Notification new user joined run",
            payload = PushJobModel(PushJobType.USER_JOINED, runSession.id.orEmpty(), map)
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_SESSION_PUSH_JOB, jobEnvelope)
        }

        val data = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Joined run",
            payload = JoinSessionQueueModel(userId, runSession.id.orEmpty()),
        )
        appScope.launch {
            // this event is to send a text message to user to let them know they have joined
            // the run successfully
            // if the session is already confirmed, we don't need to confirm again
            queueService.sendJob(QueueNames.JOINED_RUN_JOB, data)
            if(runSession.status == RunStatus.PENDING){
                // we have to trigger this event to confirm session if the number of participants have been reached
                queueService.sendJob(QueueNames.RUN_CONFIRMATION_JOB, data)
            }

        }

    }
}

class JoinSessionQueueModel(val userId:String, val runSessionId:String)