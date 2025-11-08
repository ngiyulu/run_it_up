package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@Service
class GetUserRunSessionControllerByRange: BaseController<GetUserRunSessionControllerByRangeModel, List<RunSession>>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    override fun execute(request: GetUserRunSessionControllerByRangeModel): List<RunSession> {

        if(request.userId != null){
            // this means we are getting all the runs within a timeframe where the user participated
            return getRunSessionList(request.start, request.end, request.userId, request.page)
        }
        val startUtc = request.start.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endUtc = request.end?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
        if(request.end != null){
            return runSessionRepository.findAllByDateBetween(Date.from(startUtc), Date.from(endUtc),  request.page).content.map {
                it.host = cacheManager.getAdmin(it.hostedBy.orEmpty())
                it
            }
        }

        return runSessionRepository.findAllByDatePageable(Date.from(startUtc), request.page).content.map {
            it.host = cacheManager.getAdmin(it.hostedBy.orEmpty())
            it
        }

    }



    fun getBooking(startDate: LocalDate, endDate: LocalDate?, userId:String, page: Pageable):  List<Booking>{
        val startUtc = startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endUtc = endDate?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
        if(endUtc != null){
            return bookingRepository.findAllByDateBetweenByUser(Date.from(startUtc), Date.from(endUtc), page).content
        }
        return bookingRepository.findByDate(Date.from(startUtc), page).content

    }
    fun getRunSessionList(startDate: LocalDate, endDate: LocalDate?, userId:String, page: Pageable): List<RunSession>{
        val list = mutableListOf<RunSession>()
        val booking = getBooking(startDate, endDate, userId, page)
        booking.forEach {
            val session = cacheManager.getRunSession(it.runSessionId)
            if(session != null){
                session.host = cacheManager.getAdmin(session.hostedBy.orEmpty())
                list.add(session)
            }
        }
        return  list
    }
}

class GetUserRunSessionControllerByRangeModel(
    val userId: String?,
    val start: LocalDate,
    val end: LocalDate?,
    var page: Pageable
)