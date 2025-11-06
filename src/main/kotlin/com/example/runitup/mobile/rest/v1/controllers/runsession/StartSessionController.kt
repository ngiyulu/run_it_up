package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.rest.v1.dto.session.StartSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.StartRunSessionModelEnum
import com.google.protobuf.Api
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class StartSessionController: BaseController<StartSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    @Autowired
    lateinit var sessionService: RunSessionService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    override fun execute(request: StartSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        val user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException("not_found")
        }
        var run = runDb.get()
        if(run.lockStart){
            throw ApiRequestException("run locked")
        }
       val session = sessionService.startRunSession(request, run, user.linkedAdmin)
        if(session.status ==  StartRunSessionModelEnum.INVALID_ID){
            throw ApiRequestException("invalid_id")
        }
        else if(session.status == StartRunSessionModelEnum.CONFIRMED){
            throw  ApiRequestException("invalid status")
        }
        run = session.session!!
        run.updateStatus()
        run.bookings = run.bookings.map {
            bookingDbService.getBookingDetails(it)
        }.toMutableList()
        return run
    }

}