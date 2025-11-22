package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.constants.AppConstant
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.*
import com.example.runitup.mobile.rest.v1.dto.session.LeaveSessionModel
import com.example.runitup.mobile.service.LeaveSessionService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
// user decides not to participate anymore
class LeaveSessionController: BaseController<LeaveSessionModel, RunSession>() {

    @Autowired
    lateinit var leaveSessionService: LeaveSessionService

    @Autowired
    lateinit var runService: RunSessionService

    @Autowired
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var queueService: LightSqsService


    override fun execute(request: LeaveSessionModel): RunSession {
        val user = getMyUser()
        val (booking, run) = leaveSessionService.cancelBooking(user, request.sessionId)
        val jobEnvelope = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Notification booking cancelled by user",
            payload = PushJobModel(PushJobType.BOOKING_CANCELLED_BY_USER, booking.id.orEmpty())
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_SESSION_PUSH_JOB, jobEnvelope)
        }
        runSessionEventLogger.log(
            sessionId = run.id.orEmpty(),
            action = RunSessionAction.USER_LEFT,
            actor = Actor(ActorType.USER, user.id),
            newStatus = null,
            reason = "User left session",
            correlationId = MDC.get(AppConstant.TRACE_ID),
            metadata = mapOf(AppConstant.SOURCE to MDC.get(AppConstant.SOURCE))
        )
        return runService.updateRunSession(run)
    }
}