package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.RunUser
import com.example.runitup.dto.session.JoinSessionModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.repository.UserRepository
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.PaymentService
import com.example.runitup.service.RunSessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class JoinSessionController: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private  lateinit var paymentService: PaymentService
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var sessionService: RunSessionService
    override fun execute(request: JoinSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        val auth =  SecurityContextHolder.getContext().authentication as UserPrincipal
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val run = runDb.get()
        // this mean the event is full
        if( run.atFullCapacity()){
            throw  ApiRequestException(text("full_capacity"))
        }
        if(run.atFullCapacityForGuest(request.guest)){
            throw ApiRequestException(text("spots_left", arrayOf(run.availableSpots().toString())))
        }
        val amount = request.guest * run.amount
        val newRun = sessionService.joinSession(RunUser(auth.name, auth.username, null, user.imageUrl, request.guest, false, null), run, request.stripeToken, amount.toLong())
                ?: throw ApiRequestException(text("stripe_error"))
        newRun.updateTotal()
        user.runSessions?.add(newRun)
        cacheManager.updateUser(user)
        return runSessionRepository.save(newRun).apply {
            updateButtonStatus(user.id.orEmpty())
        }
    }
}