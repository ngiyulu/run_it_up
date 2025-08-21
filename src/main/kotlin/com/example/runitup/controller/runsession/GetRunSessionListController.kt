package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.MyLocationModel
import com.example.runitup.dto.SessionListModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import com.example.runitup.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.stereotype.Service
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.data.geo.Distance
import org.springframework.security.core.context.SecurityContextHolder


@Service
class GetRunSessionListController: BaseController<SessionListModel, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: SessionListModel): List<RunSession> {
        val auth =  SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val center = Point(request.longitude, request.latitude)
        val radius = Distance(50.0, Metrics.MILES)
        val list = runSessionRepository.findByLocationNear(center, radius)
        return list.filter { (it.date.isEqual(request.date))}.map {
            it.updateButtonStatus(auth.id.orEmpty())
            it
        }
    }
}