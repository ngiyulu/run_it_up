package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.exception.ApiRequestException
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
class LeaveSessionAdminController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService


    override fun execute(request: CancelSessionModel): RunSession {
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException(text("invalid_user"))
        val auth =  SecurityContextHolder.getContext().authentication
        val principal = auth.principal as AdminPrincipal
        val admin = cacheManager.getAdmin(principal.admin.id.orEmpty())?:  throw ApiRequestException(text(text("admin_not_found")))
        return leaveSessionService.execute(request, user, admin)
    }
}