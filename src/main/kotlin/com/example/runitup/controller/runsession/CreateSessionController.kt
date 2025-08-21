package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.CreateGymRequest
import com.example.runitup.dto.CreateRunSessionRequest
import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.GymRepository
import com.example.runitup.repository.RunSessionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateSessionController: BaseController<CreateRunSessionRequest, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var gymRepository: GymRepository
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