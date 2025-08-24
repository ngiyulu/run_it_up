package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.repository.service.UserDbRepositoryService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProcessSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    lateinit var userDbRepositoryService: UserDbRepositoryService

    override fun execute(request: com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        var run = runDb.get()
//        if(run.status != RunStatus.COMPLETED){
//            throw  ApiRequestException(text("invalid_session_cancel"))
//        }
//        run.status = RunStatus.PROCESSED
//        run.playersSignedUp.forEach {
//            if(it.cancelModel?.cancelRefundType == RunUser.CancelRefundType.REFUND){
//
//            }
//        }
        run = runSessionRepository.save(run)
        return run

    }

}