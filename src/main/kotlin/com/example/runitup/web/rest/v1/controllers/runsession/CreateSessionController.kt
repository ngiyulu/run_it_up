package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.GymRepository
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.CreateRunSessionRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateSessionController: BaseController<CreateRunSessionRequest, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var gymRepository: GymRepository
    override fun execute(request: com.example.runitup.web.rest.v1.dto.CreateRunSessionRequest): RunSession {
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

        return  runSessionRepository.save(run)
    }
}