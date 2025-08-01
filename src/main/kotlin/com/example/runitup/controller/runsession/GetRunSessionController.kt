package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetRunSessionController: BaseController<String, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: String): RunSession {
        val data = runSessionRepository.findById(request)
        if(!data.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }

        return data.get().apply {
            playersSignedUp = getPlayersList().toMutableList()
        }
    }
}