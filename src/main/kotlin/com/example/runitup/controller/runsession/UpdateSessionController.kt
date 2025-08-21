package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UpdateSessionController: BaseController<RunSession, RunSession>() {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: RunSession): RunSession {
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
        run.time = request.time
        run.duration = request.duration
        run.title = request.title
        run.courtFee = request.courtFee
        run.updateTotal()
        run.increment()
        run = runSessionRepository.save(run)
        return run
    }
}