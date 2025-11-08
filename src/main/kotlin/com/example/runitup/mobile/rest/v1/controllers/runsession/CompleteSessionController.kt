package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.stereotype.Service

@Service
class CompleteSessionController: BaseController<ConfirmSessionModel, RunSession>() {


    lateinit var runSessionService: RunSessionService

    override fun execute(request: ConfirmSessionModel): RunSession {
        var run = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        if(run.status != RunStatus.ONGOING){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        run.status = RunStatus.COMPLETED
        run = runSessionService.updateRunSession(run)
        return run

    }

}