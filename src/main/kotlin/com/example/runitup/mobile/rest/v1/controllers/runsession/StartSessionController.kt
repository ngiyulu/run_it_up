package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.session.StartSessionModel
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.StartRunSessionModelEnum
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StartSessionController: BaseController<StartSessionModel, RunSession>() {


    @Autowired
    lateinit var sessionService: RunSessionService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var bookingRepository: BookingRepository

    override fun execute(request: StartSessionModel): RunSession {
        val user = getMyUser()
        var run =runSessionService.getRunSession(user.linkedAdmin != null, null, request.sessionId)?: throw ApiRequestException(text("invalid_session_id"))

        if(run.status == RunStatus.ONGOING){
            run.bookings = getBooking(run).toMutableList()
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
        run.bookings = getBooking(run).toMutableList()
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.SESSION_STARTED,
            actor = Actor(ActorType.ADMIN, user.id.orEmpty()),
            newStatus = null,
            reason = "Admin started session",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        return run
    }


    fun getBooking(runSession: RunSession):List<Booking>{
        val list = mutableListOf<Booking>()
        runSession.bookingList.forEach {
            val bookingDb = bookingRepository.findById(it.bookingId)
            if(bookingDb.isPresent){
                list.add(bookingDb.get())
            }
        }
        return  list.map {
            it.updateStatus(runSession)
            it
        }.toMutableList()
    }

}