package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProcessSessionController: BaseController<ConfirmSessionModel, RunSession>() {


    @Autowired
    lateinit var runSessionService: RunSessionService


    override fun execute(request: ConfirmSessionModel): RunSession {
        var run = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        //        if(run.status != RunStatus.COMPLETED){
//            throw  ApiRequestException(text("invalid_session_cancel"))
//        }
//        run.status = RunStatus.PROCESSED
//        run.playersSignedUp.forEach {
//            if(it.cancelModel?.cancelRefundType == RunUser.CancelRefundType.REFUND){
//
//            }
//        }
        run = runSessionService.updateRunSession(run)
        return run

    }

}