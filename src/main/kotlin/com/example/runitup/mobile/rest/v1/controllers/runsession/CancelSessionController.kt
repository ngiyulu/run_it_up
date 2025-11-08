package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.RunSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class CancelSessionController: BaseController<CancelSessionModel, RunSession>() {

    @Autowired
    lateinit var bookingDbService: BookingDbService

    @Autowired
    lateinit var runSessionService: RunSessionService

    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var appScope: CoroutineScope

    override fun execute(request: CancelSessionModel): com.example.runitup.mobile.model.RunSession {
        var run = cacheManager.getRunSession(request.sessionId) ?: throw ApiRequestException(text("invalid_session_id"))
        if(!run.isDeletable()){
            throw  ApiRequestException(text("invalid_session_cancel"))
        }
        // this is so we remeber what the status was for cancelling job
        run.statusBeforeCancel = run.status
        run.status = RunStatus.CANCELLED
        run.cancelledAt = Instant.now()
        val job = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "RAW_STRING",
            payload = run.id.orEmpty(),
            traceId = UUID.randomUUID().toString(),
            createdAtMs = Instant.now()
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_CANCELLED_JOB, job)
        }
        run = cacheManager.updateRunSession(run)
        bookingDbService.cancelAllBooking(run.id.orEmpty())
        return run
    }

}