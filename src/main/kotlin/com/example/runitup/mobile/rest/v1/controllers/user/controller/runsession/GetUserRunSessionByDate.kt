package com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.UserType
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@Service
class GetUserRunSessionByDate: BaseController<GetUserRunSessionByDateModel, List<RunSession>>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    override fun execute(request: GetUserRunSessionByDateModel): List<RunSession> {
        val auth = SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        val user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        val startUtc = request.date.atStartOfDay(ZoneOffset.UTC).toInstant()
        if(user.userType == null){
            throw ApiRequestException("not_authorized")
        }
        if(user.userType == UserType.ADMIN){
            return runSessionRepository.findAllByDatePageable(Date.from(startUtc), request.page).content.map {
                it.host = cacheManager.getAdmin(it.hostedBy.orEmpty())
                it
            }
        }
        return runSessionRepository.findAllByDateAndHostedBy(Date.from(startUtc), user.id.orEmpty(), request.page).content.map {
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
        booking.forEach { it ->
            val session = cacheManager.getRunSession(it.runSessionId)
            session?.let {
                it.host = cacheManager.getAdmin(it.hostedBy.orEmpty())
                list.add(session)
            }
        }
        return  list
    }
}

class GetUserRunSessionByDateModel(
    val date: LocalDate,
    var page: Pageable
)