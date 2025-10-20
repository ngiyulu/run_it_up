package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class GetRunSessionController: BaseController<String, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var runSessionService: RunSessionService
    override fun execute(request: String): RunSession {
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val run = runSessionService.getRunSession(request) ?: throw ApiRequestException(text("run_not_found"))
        run.updateStatus(auth.id.orEmpty())
        return run
    }
}