package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.CancelSessionModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.repository.service.RunSessionRepositoryService
import com.example.runitup.repository.service.UserRepositoryService
import com.example.runitup.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
// user decides not to participate anymore
class LeaveSessionController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var userRepositoryService: UserRepositoryService

    @Autowired
    lateinit var sessionRunSessionRepositoryService: RunSessionRepositoryService

    @Autowired
    private lateinit var sessionService: RunSessionService

    override fun execute(request: CancelSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException(text("invalid_user"))
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val run = runDb.get()
        if(!run.isDeletable()){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        userRepositoryService.deleteSessionFromUser(user.id.toString(), run.id.toString())
        return sessionRunSessionRepositoryService.setPlayerSignedUpListToCancelled(user.id.toString(), run)
    }

}