package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.GymRepository
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.service.http.MessagingService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.CreateRunSessionRequest
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateConversationModel
import model.messaging.Conversation
import model.messaging.ConversationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CreateSessionController: BaseController<CreateRunSessionRequest, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var messagingService: MessagingService

    override fun execute(request: CreateRunSessionRequest): RunSession {
        val gymDb = gymRepository.findById(request.gymId)
        if(!gymDb.isPresent){
            throw ApiRequestException("invalid_gym")
        }
        val runGym = gymDb.get()
        val run = request.runSession.apply {
            val timestamp = getTimeStamp()
            createdAt = timestamp
            status = RunStatus.PENDING
            gym = runGym
            total = 0.0
            location = runGym.location
        }
        if(run.maxPlayer < 13){
            throw ApiRequestException(text("max_player_error", arrayOf("13")))
        }
        if(run.maxGuest > 3){
            throw ApiRequestException(text("max_guest_error", arrayOf("3")))
        }
        val runSession = runSessionRepository.save(run)
        val conversation =   Conversation(UUID.randomUUID().toString(),
            ConversationType.GROUP, "",
            getTimeStamp(),
            lastMessageText = null,
            runSession = runSession.id.orEmpty(),
            lastMessageAt =  null,
            lastMessageSenderId = null,
            memberCount = 0)
        messagingService.createConversation(
            CreateConversationModel(runSession.id.orEmpty(), conversation)

        ).block()

        return  runSession
    }
}