package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RunHistoryByDateController: BaseController<String, List<RunSession>>() {

    @Autowired
    private lateinit var bookingRepository: BookingRepository
    override fun execute(request: String): List<RunSession> {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        if(user.linkedAdmin == null){
            throw ApiRequestException(text("unauthorized_user"))
        }
        val admin = cacheManager.getAdmin(user.linkedAdmin.orEmpty()) ?: throw ApiRequestException(text("admin_not_found"))
        val session = mutableListOf<RunSession>()
        val bookings = bookingRepository.findByDate(request)
        bookings.forEach {
            val run = cacheManager.getRunSession(it.runSessionId)
            if(run != null){
                run.updateAdmin(admin)
                session.add(run)
            }
        }
        // because this means the u
        session.map {
            it.updateStatus(user.id.toString())
        }
        return session
    }
}