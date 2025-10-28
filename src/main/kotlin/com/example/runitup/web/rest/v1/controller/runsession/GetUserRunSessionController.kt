package com.example.runitup.web.rest.v1.controller.runsession

import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetUserRunSessionController: BaseController<String, List<RunSession>>() {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository


    override fun execute(request: String): List<RunSession> {
        val booking = bookingRepository.findByUserId(request)
        val list: MutableList<RunSession> = mutableListOf()
        booking.forEach {
            val session = runSessionRepository.findById(it.runSessionId)
            if (session.isPresent){
                list.add(session.get())
            }
        }
        return list
    }
}