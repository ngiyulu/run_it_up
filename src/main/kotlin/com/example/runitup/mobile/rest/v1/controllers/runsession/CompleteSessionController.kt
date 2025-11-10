package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionService
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CompleteSessionController: BaseController<ConfirmSessionModel, RunSession>() {


    lateinit var runSessionService: RunSessionService
    lateinit var bookingDbService: BookingDbService

    override fun execute(request: ConfirmSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        var run = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        if(run.status != RunStatus.ONGOING){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        run.status = RunStatus.COMPLETED
        bookingDbService.completeAllBooking(run.id.orEmpty())
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.SESSION_COMPLETED,
            actor = Actor(ActorType.USER, auth.id),
            newStatus = null,
            reason = "Completion",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        run = runSessionService.updateRunSession(run)
        return run

    }

}