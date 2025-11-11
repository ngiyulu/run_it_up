package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.web.security.AdminPrincipal
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
// admin decides to remove user
class LeaveSessionAdminController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService

    override fun execute(request: CancelSessionModel): RunSession {
        val auth =  SecurityContextHolder.getContext().authentication
        val principal = auth.principal as AdminPrincipal
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException("user_not_found")
        val admin = cacheManager.getAdmin(principal.admin.id.orEmpty())?:  throw ApiRequestException(text(text("admin_not_found")))
        runSessionEventLogger.log(
            sessionId = request.sessionId,
            action = RunSessionAction.USER_KICKED_OUT,
            actor = Actor(ActorType.ADMIN, principal.admin.id.orEmpty()),
            newStatus = null,
            reason = "Admin cancel booking for user ${request.userId}",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        val (_, run) = leaveSessionService.cancelBooking(user, request.sessionId, admin)
        return run
    }
}