package com.example.runitup.web.rest.v1.controllers.runsession

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.JoinWaitListResponse
import com.example.runitup.web.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.web.rest.v1.dto.session.JoinWaitListModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class JoinWaitListController: BaseController<JoinWaitListModel, JoinWaitListResponse>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: JoinWaitListModel): JoinWaitListResponse {
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
        // user can only join the waitlist if the run is at full capacity
        if( run.atFullCapacity()){
            val runUser = com.example.runitup.web.rest.v1.dto.RunUser(
                user.firstName,
                user.lastName,
                user.skillLevel,
                user.id.orEmpty(),
                user.imageUrl,
                0,
                guest = 0
            )
            run.waitList.add(
              runUser
            )
            run.updateTotal()
            val updatedRun = runSessionRepository.save(run)
            return JoinWaitListResponse(true, updatedRun)
        }

        // this means the user tried to join the waitlist
        return  JoinWaitListResponse(false, run)

    }
}