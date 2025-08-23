package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.ConfirmSessionModel
import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.repository.service.UserDbRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CompleteSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    lateinit var userDbRepositoryService: UserDbRepositoryService

    override fun execute(request: ConfirmSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        var run = runDb.get()
        if(run.status != RunStatus.ONGOING){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        run.status = RunStatus.COMPLETED
        run = runSessionRepository.save(run)
        return run

    }

}