package com.example.runitup.web.rest.v1.controller

import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateRunSessionRequest
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.http.MessagingService
import com.example.runitup.web.security.AdminPrincipal
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateConversationModel
import model.messaging.Conversation
import model.messaging.ConversationType
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class CreateSessionController: BaseController<CreateRunSessionRequest, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var messagingService: MessagingService


    override fun execute(request: CreateRunSessionRequest): com.example.runitup.mobile.model.RunSession {
        val gymDb = gymRepository.findById(request.gymId)
        if(!gymDb.isPresent){
            throw ApiRequestException("invalid_gym")
        }
        val auth =  SecurityContextHolder.getContext().authentication
        val savedAdmin = auth.principal as AdminPrincipal
        val runGym = gymDb.get()
        val run = RunSession(
            id = ObjectId().toString(),
            gym = runGym,
            location = runGym.location,
            date = request.date,
            startTime = request.startTime,
            endTime = request.endTime,
            zoneId = request.zoneId,
            hostedBy = savedAdmin.admin.id,
            allowGuest = request.allowGuest,
            notes = request.notes,
            privateRun = request.privateRun,
            title = request.title,
            maxGuest = request.maxGuest,
            maxPlayer = request.maxPlayer,
            description = request.description,
            duration = 0
            ).apply {
            val timestamp = getTimeStamp()
            createdAt = timestamp
            status = RunStatus.PENDING
            gym = runGym
            total = 0.0
            location = runGym.location
        }
        if(run.maxPlayer < 10){
            throw ApiRequestException(text("max_player_error", arrayOf("10")))
        }
        if(run.maxGuest >10){
            throw ApiRequestException(text("max_guest_error", arrayOf("10")))
        }
        val runSession = runSessionRepository.save(run)
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

        return  runSession
    }
}