package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
// user decides not to participate anymore
class LeaveSessionController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService

    @Autowired
    lateinit var runService: RunSessionService

    @Autowired
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var queueService: LightSqsService


    override fun execute(request: CancelSessionModel): RunSession {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val (booking, run) = leaveSessionService.cancelBooking(user, request.sessionId)
        val jobEnvelope = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Notification booking cancelled by user",
            payload = PushJobModel(PushJobType.BOOKING_CANCELLED_BY_ADMIN, booking.id.orEmpty())
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_SESSION_PUSH_JOB, jobEnvelope)
        }
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.USER_LEFT,
            actor = Actor(ActorType.USER, auth.id),
            newStatus = null,
            reason = "User left session",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        return runService.updateRunSession(run)
    }
}