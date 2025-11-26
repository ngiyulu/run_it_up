package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.Coordinate
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.SessionListModel
import com.example.runitup.mobile.service.LightSqsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*


@Service
class GetRunSessionListController: BaseController<SessionListModel, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var appScope: CoroutineScope
    override fun execute(request: SessionListModel): List<RunSession> {
        val user = getMyUser()
        val radius = 20.0
        logger.info("longitude = ${request.longitude}")
        logger.info("latitude = ${request.latitude}")
        val userZone = request.zoneId // 👈 from client (device timezone)
        val startUtc = request.date.atStartOfDay(userZone).toInstant()
        val endUtc = request.date.plusDays(1)?.atStartOfDay(userZone)?.toInstant()
        val maxDistanceMeters = radius * 1609.344 // 32186.88
        val statuses = listOf(RunStatus.PENDING, RunStatus.PROCESSED, RunStatus.ONGOING, RunStatus.CONFIRMED)
        val page = runSessionRepository.findJoinableRunsExcludingUserNearOnLocalDay(
            userId = user.id.orEmpty(),
            lat = request.latitude,
            lng = request.longitude,
            maxDistanceMeters = maxDistanceMeters,
            statuses = statuses,
            pageable = request.pageRequest,
            startInclusive = Date.from(startUtc),
            endExclusive = Date.from(endUtc)
        )

        val payload = CoordinateUpdateModel(user.id.orEmpty(), Coordinate(request.longitude.toLong(), request.latitude.toLong()))
        val data = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "User location update",
            payload = payload
        )
        appScope.launch {
            queueService.sendJob(QueueNames.LOCATION_JOB, data)
        }
        val content = page.content.map {
            it.updateStatus(user.id.orEmpty())
            it
        }
        return  content
    }
}

data class CoordinateUpdateModel(val userId: String , val coordinate: Coordinate)