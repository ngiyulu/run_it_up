package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.SessionListModel
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
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
        val center = Point(request.longitude, request.latitude)
        val radius = Distance(50.0, Metrics.MILES)
        val list = runSessionRepository.findByLocationNear(center, radius)
        return list.filter {
            (it.date.isEqual(request.date)) && !it.isParticiPant(auth.id.orEmpty()) && !it.isWaitlisted(auth.id.orEmpty()) }
            .map {
                it.updateStatus(auth.id.orEmpty())
                it
            }
    }
}