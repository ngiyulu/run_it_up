package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.RunSessionService
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