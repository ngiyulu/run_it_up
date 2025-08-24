package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.session.JoinSessionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class JoinWaitListController: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: com.example.runitup.web.rest.v1.dto.session.JoinSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run = runDb.get()
        // this mean the event is full
        if( user.stripeId == null){
            throw ApiRequestException(text("payment_error"))
        }
        if( !run.isJoinable()){
            throw  ApiRequestException(text("join_error"))
        }
        // this means the run is full, so we return the run to the user
        // and the ui will update, this should only happen if they had an old version of the run
        // that did not have the proper ui
        val availableSpots = run.availableSpots()
        if( run.atFullCapacity() || availableSpots < request.guest){
            val runUser = com.example.runitup.web.rest.v1.dto.RunUser(
                user.firstName,
                user.lastName,
                user.skillLevel,
                user.id.orEmpty(),
                user.imageUrl,
                0,
                guest = request.guest
            )
            run.waitList.add(
              runUser
            )
            run.updateTotal()
            return runSessionRepository.save(run)
        }

        return  run

    }
}