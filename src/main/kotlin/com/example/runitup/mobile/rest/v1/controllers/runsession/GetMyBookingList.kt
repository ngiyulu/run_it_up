package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetMyBookingList: BaseController<String, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository
    override fun execute(request: String): List<RunSession> {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        val session = mutableListOf<RunSession>()
        val bookings = bookingRepository.findByUserIdAndStatusIn(request,  mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED, BookingStatus.JOINED))
        bookings.forEach {
            val dbRes = runSessionRepository.findById(it.runSessionId)
            if(dbRes.isPresent){
              session.add(dbRes.get())
            }
        }

        // because this means the u
        session.map {
            it.updateStatus(user.id.toString())
        }
        return session
    }
}