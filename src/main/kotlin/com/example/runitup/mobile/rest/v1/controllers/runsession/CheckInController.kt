package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.CheckIn
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CheckInController: BaseController<CheckIn, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var runSessionService: RunSessionService

    override fun execute(request: CheckIn): com.example.runitup.mobile.model.RunSession {
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
        return runSessionService.updateRunSession(run).apply {
            updateStatus(auth.id.orEmpty())
        }
    }
}