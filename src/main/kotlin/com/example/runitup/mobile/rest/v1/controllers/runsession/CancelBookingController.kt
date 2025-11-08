package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.common.model.AdminUser
import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelBookingModel
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
// admin decides to remove user
class CancelBookingController: BaseController<CancelBookingModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository
    @Autowired
    lateinit var runService: RunSessionService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    @Autowired
    lateinit var runSessionPushNotificationService: RunSessionPushNotificationService

    override fun execute(request: CancelBookingModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication
        val savedPrincipal = auth.principal
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException("user_not_found")
        // this means the service call was called from web portal
        if (savedPrincipal is AdminPrincipal){
            val admin = adminUserRepository.findById(savedPrincipal.admin.id.orEmpty())
            if(!admin.isPresent){
                throw ApiRequestException("user_not_found")
            }
          return complete(user, request.sessionId, admin.get())
        }

        if(user.linkedAdmin == null){
            throw ApiRequestException("not_authorized")
        }
        val admin = adminUserRepository.findById(user.linkedAdmin.orEmpty())
        if(!admin.isPresent){
            throw ApiRequestException("user_not_found")
        }
        return complete(user, request.sessionId, admin.get())
    }

    private fun complete(user: User, sessionId:String, admin: AdminUser?): RunSession{
        val run = leaveSessionService.cancelBooking(user, sessionId, admin)
        run.updateStatus()
        // we need to return this for admin
        run.bookings = run.bookings.map {
            bookingDbService.getBookingDetails(it)
        }.toMutableList()
        runSessionPushNotificationService.runSessionBookingCancelled(user.id.orEmpty(), run)
        return runService.updateRunSession(run)
    }
}