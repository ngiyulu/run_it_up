package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.session.CancelSessionModel
import com.example.runitup.enum.RunStatus
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.repository.service.BookingDbService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CancelSessionController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var bookingDbService: BookingDbService

    override fun execute(request: CancelSessionModel): RunSession {
        val runDb = runSessionRepository.findById(request.sessionId)
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        var run = runDb.get()
        if(!run.isDeletable()){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        run.status = RunStatus.CANCELLED
        if(run.status == RunStatus.CONFIRMED){
            //TODO: add  to queue for refunding
        }
        run = runSessionRepository.save(run)
        bookingDbService.cancelAllBooking(run.id.orEmpty())
        return run
    }

}