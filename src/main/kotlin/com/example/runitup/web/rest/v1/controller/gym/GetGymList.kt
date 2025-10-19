package com.example.runitup.web.rest.v1.controller.gym

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.CreateRunSessionRequest
import com.example.runitup.mobile.service.http.MessagingService
import com.ngiyulu.runitup.messaging.runitupmessaging.dto.conversation.CreateConversationModel
import model.messaging.Conversation
import model.messaging.ConversationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class GetGymList: BaseController<Unit, List<Gym>>() {

    @Autowired
    lateinit var gymRepository: GymRepository


    override fun execute(request: Unit): List<Gym> {
        return gymRepository.findAll().filter { it.isActive }.sortedBy { it.title.lowercase() }
    }
}