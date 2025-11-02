package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.SessionListModel
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.util.Date


@Service
class GetRunSessionListController: BaseController<SessionListModel, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: SessionListModel): List<RunSession> {
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val radius = 20.0
        val startUtc = request.date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endUtc = request.date.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
        val maxDistanceMeters = radius * 1609.344 // 32186.88
        val statuses = listOf(RunStatus.PENDING, RunStatus.PROCESSED, RunStatus.ONGOING)
        val page = runSessionRepository.findJoinableRunsExcludingUserNearOnLocalDay(
            userId = auth.id.orEmpty(),
            lat = request.latitude,
            lng = request.longitude,
            maxDistanceMeters = maxDistanceMeters,
            statuses = statuses,
            pageable = request.pageRequest,
            startInclusive = Date.from(startUtc),
            endExclusive = Date.from(endUtc)
        )
        val content = page.content.map {
            it.updateStatus(auth.id.orEmpty())
            it
        }
        return  content
    }
}