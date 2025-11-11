package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.NumberGenerator
import com.example.runitup.mobile.service.RunSessionService
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
/// confirm session is when we charge the card, this happens 3 hours before the run
class ConfirmSessionController: BaseController<ConfirmSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var bookingDbService: BookingDbService


    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var numberGenerator: NumberGenerator

    override fun execute(request: ConfirmSessionModel): RunSession {
        val auth =  SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        val user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        var run =runSessionService.getRunSession(request.sessionId)?: throw ApiRequestException(text("invalid_session_id"))
        if(run.status == RunStatus.CONFIRMED){
            run.bookings = run.bookings.map {
                bookingDbService.getBookingDetails(it)
            }.toMutableList()
            return run
        }
        if(run.status != RunStatus.PENDING){
            logger.error("user is trying to confirm a run session that's not in pending for run {}", run.id)
            return run
        }
        if(run.bookings.isEmpty()){
            throw ApiRequestException(text("no booking"))
        }
        if(!request.overrideMinimum && run.bookings.size < run.minimumPlayer){
            throw ApiRequestException(text("min_player"))
        }
        if(run.privateRun && run.code != null && !request.isAdmin){
            if(!numberGenerator.validateEncryptedCode(run.code!!, request.code)){
                throw ApiRequestException(text("invalid_code"))
            }
        }
        run = runSessionService.startConfirmationProcess(run, user.id.orEmpty())
        if(request.isAdmin){
            run.updateStatus()
            run.bookings = run.bookings.map {
                bookingDbService.getBookingDetails(it)
            }.toMutableList()
        }
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.SESSION_CONFIRMED,
            actor = Actor(ActorType.USER, savedUser.id),
            newStatus = null,
            reason = "Confirmation",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        return  run
    }

}