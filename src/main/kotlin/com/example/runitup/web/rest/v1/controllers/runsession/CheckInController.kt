package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.user.CheckIn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CheckInController: BaseController<CheckIn, RunSession>() {   @Autowired

    lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: com.example.runitup.web.rest.v1.dto.user.CheckIn): RunSession {
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val run = runDb.get()
//        val playerList = run.getPlayersList()
//        val signedUpPlayer = playerList.findLast { it.userId == request.userId }
//        if (signedUpPlayer != null) {
//            logger.logError("", "Player signedup for with id=${request.userId} not found")
//            throw ApiRequestException(text("player_not_found"))
//        }
//        signedUpPlayer?.checkIn = true
//        signedUpPlayer?.status = RunUser.RunUserStatus.PLAYED
        return runSessionRepository.save(run).apply {
            updateStatus(auth.id.orEmpty())
        }
    }
}