package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.common.model.AdminUser
import com.example.runitup.common.repo.AdminUserRepository
import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.session.CancelBookingModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.web.security.AdminPrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

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
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var bookingDbService: BookingDbService

    override fun execute(request: CancelBookingModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication
        val savedPrincipal = auth.principal
        // this means the service call was called from web portal
        val user = cacheManager.getUser(request.userId) ?: throw ApiRequestException("user_not_found")
        if (savedPrincipal is AdminPrincipal){
            val admin = adminUserRepository.findById(savedPrincipal.admin.id.orEmpty())
            if(!admin.isPresent){
                logger.error("admin is not found from web flow")
                throw ApiRequestException("user_not_found")
            }
            return complete(user, request.sessionId, admin.get())
        }
        val adminUser = cacheManager.getUser((savedPrincipal as UserPrincipal).user.id.orEmpty()) ?: throw ApiRequestException("user_not_found")
        if(adminUser.linkedAdmin == null){
            logger.error("admin is not found from app flow")
            throw ApiRequestException("not_authorized")
        }
        val admin = adminUserRepository.findById(adminUser.linkedAdmin.orEmpty())
        if(!admin.isPresent){
            logger.error("admin is not found from app flow")
            throw ApiRequestException("user_not_found")
        }
        return complete(user, request.sessionId, admin.get())
    }

    private fun complete(user: User, sessionId:String, admin: AdminUser?): RunSession{
        val (booking, run )= leaveSessionService.cancelBooking(user, sessionId, admin)
        run.updateStatus()
        // we need to return this for admin
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.BOOKING_CANCELLED_BY_ADMIN,
            actor = Actor(ActorType.ADMIN, admin?.id.orEmpty()),
            newStatus = null,
            reason = "Admin cancelled booking",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        val jobEnvelope = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Notification booking cancelled by admin",
            payload = PushJobModel(PushJobType.BOOKING_CANCELLED_BY_ADMIN, booking.id.orEmpty())
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_SESSION_PUSH_JOB, jobEnvelope,  delaySeconds = 0)
        }
        val updatedRun = runService.updateRunSession(run)
        val bookings = run.bookings.map {
            bookingDbService.getBookingDetails(it)
        }.toMutableList()
        updatedRun.bookings = bookings
        return updatedRun
    }
}