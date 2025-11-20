package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.config.AppConfig
import com.example.runitup.mobile.constants.AppConstant.TRACE_ID
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.controllers.UserModelType
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.CreateRunSessionRequest
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.service.NumberGenerator
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.web.security.AdminPrincipal
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateConversationModel
import model.messaging.Conversation
import model.messaging.ConversationType
import org.bson.types.ObjectId
import org.jboss.logging.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@Service
class CreateRunSessionController: BaseController<CreateRunSessionRequest, RunSession>() {



    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var messagingService: MessagingService

    @Autowired
    lateinit var paymentConfig: AppConfig

    @Autowired
    lateinit var numberGenerator: NumberGenerator


    override fun execute(request: CreateRunSessionRequest): RunSession {
        val gymDb = gymRepository.findById(request.gymId)
        if(!gymDb.isPresent){
            throw ApiRequestException("invalid_gym")
        }
        var host = ""
        val user = getUser()
        val admin: AdminUser
        if(user.type == UserModelType.ADMIN){
            host = user.userId
            admin = user.adminUser!!
        }
        else{
            var localUser= user.user!!
            if(localUser.linkedAdmin == null){
                throw ApiRequestException(text("unauthorized_user"))
            }
            admin = cacheManager.getAdmin(localUser.linkedAdmin.orEmpty()) ?: throw ApiRequestException(text("admin_not_found"))
        }
        val runGym = gymDb.get()

        // 1) Parse the request date/time *as local values* in the given zone
        val localDate = LocalDate.parse(request.date)                    // "2025-11-17"
        val localStartTime = LocalTime.parse(request.startTime)          // "12:45"
        val localEndTime = LocalTime.parse(request.endTime)              // "14:45"

        if (localStartTime.isAfter(localEndTime)) {
            throw ApiRequestException("time_invalid")
        }
        print("timezone = ${runGym.zoneId}")
        // 2) Compute startAtUtc based on the gym's time zone
        val zone = ZoneId.of(runGym.zoneId) // e.g. "America/Chicago"
        val startAtUtc = localDate
            .atTime(localStartTime)
            .atZone(zone)
            .toInstant()
        val run = RunSession(
            id = ObjectId().toString(),
            gym = runGym,
            location = runGym.location,
            date = localDate,
            //is a proper UTC Instant, consistent across local laptop and server.
            startTime = localStartTime,
            endTime = localEndTime,
            zoneId = runGym.zoneId,
            hostedBy = host,
            allowGuest = request.allowGuest,
            notes = request.notes,
            privateRun = request.privateRun,
            title = request.title,
            maxGuest = request.maxGuest,
            amount = request.fee,
            minimumPlayer = request.minPlayer,
            maxPlayer = request.maxPlayer,
            description = request.description,
            duration = 0,
            startAtUtc = startAtUtc
        ).apply {
            createdAt = Instant.now()
            status = RunStatus.PENDING
            gym = runGym
            total = 0.0
            location = runGym.location
        }
        if(request.privateRun){
            run.code = numberGenerator.encryptCode(5)
        }

        if(!paymentConfig.payment){
            run.amount = 0.0
        }
        if(run.minimumPlayer >= run.maxPlayer){
            throw ApiRequestException(text("invalid_min_player"))
        }
        if(run.maxPlayer < 10){
            throw ApiRequestException(text("max_player_error", arrayOf("10")))
        }
        if(run.maxGuest >10){
            throw ApiRequestException(text("max_guest_error", arrayOf("10")))
        }
        // if the payment feature is disabled, the run session has to be free or block it
        if(!run.isSessionFree() && !paymentConfig.payment){
            throw ApiRequestException(text("payment_disabled"))
        }

        val runSession = cacheManager.updateRunSession(run)

        val conversation =   Conversation(UUID.randomUUID().toString(),
            ConversationType.GROUP,
            "",
            getTimeStamp(),
            lastMessageText = null,
            runSession = runSession.id.orEmpty(),
            lastMessageAt =  null,
            lastMessageSenderId = null,
            memberCount = 0,
            title = runSession.getConversationTitle())
        messagingService.createConversation(
            CreateConversationModel(runSession.id.orEmpty(), conversation)
        ).block()
        val reason = if(request.isAdmin){
            "Admin created session"
        } else{
            "Admin created session from mobile app"
        }
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.SESSION_CREATED,
            actor = Actor(ActorType.ADMIN, admin.id.orEmpty()),
            newStatus = "CREATED",
            reason = reason,
            correlationId = MDC.get(TRACE_ID) as? String,
            idempotencyKey = "session:create:${run.id}"
        )

        return  runSession
    }
}