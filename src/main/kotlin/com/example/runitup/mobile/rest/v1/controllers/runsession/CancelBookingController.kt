package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
// admin decides to remove user
class CancelBookingController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService

    override fun execute(request: CancelSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication
        val savedAdmin = auth.principal as AdminPrincipal
        return leaveSessionService.execute(request, savedAdmin.admin)
    }
}