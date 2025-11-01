package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.SessionListModel
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service


@Service
class GetRunSessionListController: BaseController<SessionListModel, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: SessionListModel): List<RunSession> {
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val radius = 20.0

        val maxDistanceMeters = radius * 1609.344 // 32186.88
        val statuses = listOf(RunStatus.PENDING, RunStatus.PROCESSED, RunStatus.ONGOING)
        val page = runSessionRepository.findJoinableRunsExcludingUserNearOnDate(
            userId = auth.id.orEmpty(),
            lat = request.latitude,
            lng = request.longitude,
            maxDistanceMeters = maxDistanceMeters,
            statuses = statuses,
            pageable = request.pageRequest,
            date = request.date
        )
        // TODO: store the coordinate of user
        return  page.content

    }
}