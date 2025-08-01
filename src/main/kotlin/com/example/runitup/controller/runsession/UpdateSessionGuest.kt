package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.JoinSessionModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.service.PaymentService
import com.example.runitup.service.RunSessionService
import com.stripe.model.Charge
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UpdateSessionGuest: BaseController<JoinSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private  lateinit var paymentService: PaymentService

    @Autowired
    lateinit var runSessionService: RunSessionService
    override fun execute(request: JoinSessionModel): RunSession {
        val user = getUser()
        var isUserAddingGuest = false
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        val run = runDb.get()
        if(run.atFullCapacity()){
            //TODO: later we can find a way to add the guest to the waitlist
            throw ApiRequestException(text("full_capacity"))
        }
        val signedUpUser = run.getPlayersList().findLast { it.userId == user.id.toString() }
        if(signedUpUser == null){
            logger.logError("error updating session, user is null",  null)
            throw ApiRequestException(text("invalid params"))
        }
        if(signedUpUser.stripeChargeId == null){
            logger.logError("stripe charge is null",  null)
            throw ApiRequestException(text("invalid params"))
        }
        if(request.guest > signedUpUser.guest){
            isUserAddingGuest = true
        }
        if(isUserAddingGuest && run.atFullCapacity()){
            throw  ApiRequestException(text("full_capacity"))
        }
        val amount = request.guest * run.amount
        return runSessionService.updateSessionGuest(request.guest, signedUpUser, run, signedUpUser.stripeChargeId.orEmpty(), amount.toLong())
            ?: throw ApiRequestException(text("stripe_error"))
    }
}