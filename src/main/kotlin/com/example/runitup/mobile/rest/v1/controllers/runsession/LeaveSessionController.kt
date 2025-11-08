package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
// user decides not to participate anymore
class LeaveSessionController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService

    @Autowired
    lateinit var runService: RunSessionService


    override fun execute(request: CancelSessionModel): RunSession {
        val user = cacheManager.getUser(request.userId)?: throw ApiRequestException("user_not_found")
        val run = leaveSessionService.cancelBooking(user, request.sessionId)
        return runService.updateRunSession(run)
    }
}