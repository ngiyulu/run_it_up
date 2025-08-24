package com.example.runitup.web.rest.v2.controllers.runsession

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.BookingRepository
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v2.dto.MyBookingModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetMyBookingListV2: BaseController<String, List<MyBookingModel>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository
    override fun execute(request: String): List<MyBookingModel> {
        val user = cacheManager.getUser(request) ?: throw ApiRequestException(text("user_not_found"))
        val session = mutableListOf<RunSession>()
        val bookings = bookingRepository.findByUserId(request)
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
        val grouped = session.groupBy { it.date }
        return grouped
            .toSortedMap()
            .map { (date, session) ->
                MyBookingModel(date, session.map { it })
            }

    }
}