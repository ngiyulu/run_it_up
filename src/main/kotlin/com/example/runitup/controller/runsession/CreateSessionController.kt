package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.enum.RunStatus
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateSessionController: BaseController<RunSession, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: RunSession): RunSession {
        val timestamp = getTimeStamp()
        request.createdAt = timestamp
        request.status = RunStatus.PENDING
        return  runSessionRepository.save(request)
    }
}