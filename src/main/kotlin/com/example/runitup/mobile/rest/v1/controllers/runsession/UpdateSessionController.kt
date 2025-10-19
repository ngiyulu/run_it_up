package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UpdateSessionController: BaseController<RunSession, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: com.example.runitup.mobile.model.RunSession): com.example.runitup.mobile.model.RunSession {
        val runDb = runSessionRepository.findById(request.id.toString())
        if(!runDb.isPresent){
            throw ApiRequestException(text("invalid_session_id"))
        }
        var run = runDb.get()
        if(!run.isUpdatable()){
            throw ApiRequestException(text("error"))
        }
        run.gym = request.gym
        run.updatedAt = getTimeStamp()
        run.startTime = request.startTime
        run.endTime = request.endTime
        run.duration = request.duration
        run.title = request.title
        run.notes = request.notes
        run.description = request.description
        run.courtFee = request.courtFee
        run.increment()
        run = runSessionRepository.save(run)
        return run
    }
}