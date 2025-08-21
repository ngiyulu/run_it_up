package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.SessionListModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetMyBookingList: BaseController<String, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: String): List<RunSession> {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        val session = mutableListOf<RunSession>()
        user.runSessions.orEmpty().forEach {
            val dbRes = runSessionRepository.findById(it.id.orEmpty())
            if(!dbRes.isPresent){
              session.add(dbRes.get())
            }
        }
        return session
    }
}