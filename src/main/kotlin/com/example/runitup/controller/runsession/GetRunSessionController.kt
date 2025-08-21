package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class GetRunSessionController: BaseController<String, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: String): RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val data = runSessionRepository.findById(request)
        if(!data.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val run = data.get()
        run.playersSignedUp = run.playersSignedUp.toMutableList()
        run.updateButtonStatus(user.id.toString())
        return run
    }
}