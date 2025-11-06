package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.StartRunSessionModelEnum
import com.google.protobuf.Api
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    @Autowired
    lateinit var sessionService: RunSessionService

    override fun execute(request: ConfirmSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException("not_found")
        }
        val run = runDb.get()
        if(run.lockStart){
            throw ApiRequestException("run locked")
        }
       val session = sessionService.startRunSession(request, run)
        if(session.status ==  StartRunSessionModelEnum.INVALID_ID){
            throw ApiRequestException("invalid_id")
        }
        else if(session.status == StartRunSessionModelEnum.CONFIRMED){
            throw  ApiRequestException("invalid status")
        }
        return  session.session!!
    }

}