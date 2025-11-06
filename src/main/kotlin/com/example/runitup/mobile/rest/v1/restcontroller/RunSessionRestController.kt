package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.rest.v1.controllerprovider.SessionControllersProvider
import com.example.runitup.mobile.rest.v1.controllers.user.controller.runsession.CreateRunSessionController
import com.example.runitup.mobile.rest.v1.dto.CreateRunSessionRequest
import com.example.runitup.mobile.rest.v1.dto.JoinRunSessionResponse
import com.example.runitup.mobile.rest.v1.dto.JoinWaitListResponse
import com.example.runitup.mobile.rest.v1.dto.SessionListModel
import com.example.runitup.mobile.rest.v1.dto.session.CancelSessionModel
import com.example.runitup.mobile.rest.v1.dto.session.ConfirmSessionModel
import com.example.runitup.mobile.rest.v1.dto.session.JoinSessionModel
import com.example.runitup.mobile.rest.v1.dto.session.JoinWaitListModel
import com.example.runitup.mobile.rest.v1.dto.stripe.CreatePIRequest
import com.example.runitup.mobile.rest.v1.dto.stripe.CreatePIResponse
import com.example.runitup.mobile.rest.v1.dto.user.CheckIn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId

@RequestMapping("/api/v1/run-session")
@RestController
class RunSessionRestController {

    @Autowired
    lateinit var createSessionController: CreateRunSessionController

    @Autowired
    lateinit var sessionControllersProvider: SessionControllersProvider

    @PostMapping("/join")
    fun join(@RequestBody model: JoinSessionModel): JoinRunSessionResponse {
        return sessionControllersProvider.joinSessionController.execute(model)
    }

    @PostMapping("/waitlist")
    fun joinWaitList(@RequestBody model: JoinWaitListModel): JoinWaitListResponse {
        return sessionControllersProvider.joinWaitListController.execute(model)
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody model: ConfirmSessionModel): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.confirmSessionController.execute(model)
    }

    @PostMapping("/update")
    fun update(@RequestBody model: com.example.runitup.mobile.model.RunSession): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.updateSessionController.execute(model)
    }

    @GetMapping("/retrieve/{id}")
    fun getSession(@PathVariable id:String): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.getRunSessionController.execute(id)
    }

    @GetMapping("/booking/{id}")
    fun getMyBooking(@PathVariable id:String):List<com.example.runitup.mobile.model.RunSession> {
        return sessionControllersProvider.getMyBookingSessionController.execute(id)
    }

    @GetMapping("/list")
    fun getSessionList( @RequestParam(required = true) long: Double,
                        @RequestParam(required = true) lat: Double,
                        @RequestParam("date")
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
                        @RequestParam(defaultValue = "0") page: Int,
                        @RequestParam(defaultValue = "25") size: Int,
                        @RequestHeader("X-Timezone", required = true) tzHeader: String
    ):List<com.example.runitup.mobile.model.RunSession> {
        return sessionControllersProvider.getRunSessionListController.execute(
            SessionListModel(
                longitude = long,
                latitude = lat,
                date = date,
                pageRequest = PageRequest.of(page, size),
                zoneId = ZoneId.of(tzHeader)

            )
        )
    }

    @PostMapping("/check-in")
    fun checkInLevel(@RequestBody model: CheckIn): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.checkInController.execute(model)
    }

    @PostMapping("/cancel")
    fun checkInLevel(@RequestBody model: CancelSessionModel): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.cancelSessionController.execute(model)
    }

    @PostMapping("/complete")
    fun completeSession(@RequestBody model: ConfirmSessionModel): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.completeSessionController.execute(model)
    }

    @PostMapping("/leave")
    fun leaveSession(@RequestBody model: CancelSessionModel): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.leaveSessionController.execute(model)
    }

    @PostMapping("/start")
    fun startSession(@RequestBody model: ConfirmSessionModel): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.startSessionController.execute(model)
    }

    @PostMapping("/update/guest")
    fun updateSessionGuest(@RequestBody model: JoinSessionModel): com.example.runitup.mobile.model.RunSession {
        return sessionControllersProvider.updateSessionGuest.execute(model)
    }

    @PostMapping("/payment/intent")
    fun createPaymentIntent(@RequestBody model: CreatePIRequest): CreatePIResponse {
        return sessionControllersProvider.createPaymentIntent.execute(model)
    }

    @PostMapping("/create")
    fun create(@RequestBody model: CreateRunSessionRequest): RunSession {
        return createSessionController.execute(model)
    }
}