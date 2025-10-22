package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.CreateRunSessionRequest
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.service.RunSessionService
import com.example.runitup.web.rest.v1.controller.runsession.CreateSessionController
import com.example.runitup.web.rest.v1.controller.runsession.GetUserRunSessionController
import com.example.runitup.web.rest.v1.controller.runsession.LeaveSessionAdminController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*


@RestController
@RequestMapping("/admin/api/v1/run-session")
class AdminRunSessionRestController {

    @Autowired
    lateinit var repo: RunSessionRepository

    @Autowired
    lateinit var cacheManager: MyCacheManager

    @Autowired
    lateinit var createSessionController: CreateSessionController

    @Autowired
    lateinit var leaveSessionAdminController: LeaveSessionAdminController

    @Autowired
    lateinit var runSessionController: GetUserRunSessionController

    @Autowired
    lateinit var runSessionService: RunSessionService
    @PostMapping("/create")
    fun create(@RequestBody model: CreateRunSessionRequest): RunSession {
        return createSessionController.execute(model)
    }

    @GetMapping("/by-date/{date}")
    fun byDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): List<RunSession> {
        val startUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endUtc = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        return repo.findAllByDateBetween(Date.from(startUtc), Date.from(endUtc)).map {
            it.host = cacheManager.getAdmin(it.hostedBy.orEmpty())
            it
        }
        // If you used the Instant overload, pass startUtc/endUtc directly.
    }

    @GetMapping("/by-range")
    fun byRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate
    ): List<RunSession> {
        val startUtc = start.atStartOfDay(ZoneOffset.UTC).toInstant()
        val endUtc = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        return repo.findAllByDateBetween(Date.from(startUtc), Date.from(endUtc)).map {
            it.host = cacheManager.getAdmin(it.hostedBy.orEmpty())
            it
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): RunSession? {
        val runSession = runSessionService.getRunSession(id) ?: throw ApiRequestException("invalid_gym")
        runSession.host = cacheManager.getAdmin(runSession.hostedBy.orEmpty())
        return runSession
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String) = repo.deleteById(id)

    @GetMapping("/{id}/list")
    fun listRunSession(@PathVariable id: String): List<RunSession> {
        return runSessionController.execute(id)
    }

    @PostMapping("/remove")
    fun leaveSession(@RequestBody model: CancelSessionModel): RunSession {
        var runSession= leaveSessionAdminController.execute(model)
        runSession = runSessionService.getRunSession(runSession.id.orEmpty()) ?: throw ApiRequestException("invalid_gym")
        runSession.host = cacheManager.getAdmin(runSession.hostedBy.orEmpty())
        return runSession
    }
}