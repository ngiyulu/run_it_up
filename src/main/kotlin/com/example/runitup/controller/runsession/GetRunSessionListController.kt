package com.example.runitup.controller.runsession

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.MyLocationModel
import com.example.runitup.dto.SessionListModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.RunSession
import com.example.runitup.repository.RunSessionRepository
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.stereotype.Service
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.data.geo.Distance


@Service
class GetRunSessionListController: BaseController<SessionListModel, List<RunSession>>() {

    @Autowired
    private lateinit var runSessionRepository: RunSessionRepository
    override fun execute(request: SessionListModel): List<RunSession> {
        val center = Point(request.longitude, request.latitude)
        val radius = Distance(50.0, Metrics.MILES)
        return runSessionRepository.findByLocationNear(center, radius)
    }
}