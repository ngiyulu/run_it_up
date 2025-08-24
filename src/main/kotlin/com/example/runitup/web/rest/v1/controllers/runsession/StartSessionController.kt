package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    override fun execute(request: com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        var run = runDb.get()
        if(run.status != RunStatus.PENDING){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        run.status = RunStatus.ONGOING
        run = runSessionRepository.save(run)
        return run


    }

}