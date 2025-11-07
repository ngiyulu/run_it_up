package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
// admin decides to remove user
class CancelBookingController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService
    @Autowired
    lateinit var runService: RunSessionService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    override fun execute(request: CancelSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication
        val savedPrincipal = auth.principal
        if (savedPrincipal is AdminPrincipal){
          return complete(request, savedPrincipal.admin)
        }
        return complete(request, null)
    }

    private fun complete(request: CancelSessionModel, admin: AdminUser?): RunSession{
        val run = leaveSessionService.execute(request, admin)
        run.updateStatus()
        // we need to return this for admin
        run.bookings = run.bookings.map {
            bookingDbService.getBookingDetails(it)
        }.toMutableList()
        return runService.updateRunSession(run)
    }
}