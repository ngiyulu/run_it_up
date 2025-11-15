package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetRunSessionController: BaseController<String, RunSession>() {

    @Autowired
    lateinit var runSessionService: RunSessionService
    override fun execute(request: String): RunSession {
        val user =getMyUser()
        val run = runSessionService.getRunSession(user.linkedAdmin!= null, null, request, user.id.orEmpty()) ?: throw ApiRequestException(text("run_not_found"))
        run.updateStatus(user.id.orEmpty())
        return run
    }
}