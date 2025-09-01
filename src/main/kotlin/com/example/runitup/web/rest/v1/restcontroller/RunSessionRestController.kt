package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.model.RunSession
import com.example.runitup.web.rest.v1.controllerprovider.SessionControllersProvider
import com.example.runitup.web.rest.v1.dto.JoinRunSessionResponse
import com.example.runitup.web.rest.v1.dto.JoinWaitListResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RequestMapping("/api/v1/run-session")
@RestController
class RunSessionRestController {

    @Autowired
    lateinit var sessionControllersProvider: SessionControllersProvider
    @PostMapping("/create")
    fun create(@RequestBody model: com.example.runitup.web.rest.v1.dto.CreateRunSessionRequest):RunSession {
        return sessionControllersProvider.createSessionController.execute(model)
    }

    @PostMapping("/join")
    fun join(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.JoinSessionModel):JoinRunSessionResponse {
        return sessionControllersProvider.joinSessionController.execute(model)
    }

    @PostMapping("/waitlist")
    fun joinWaitList(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.JoinWaitListModel):JoinWaitListResponse {
        return sessionControllersProvider.joinWaitListController.execute(model)
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel):RunSession {
        return sessionControllersProvider.confirmSessionController.execute(model)
    }

    @PostMapping("/update")
    fun update(@RequestBody model: RunSession):RunSession {
        return sessionControllersProvider.updateSessionController.execute(model)
    }

    @GetMapping("/retrieve/{id}")
    fun getSession(@PathVariable id:String):RunSession {
        return sessionControllersProvider.getRunSessionController.execute(id)
    }

    @GetMapping("/booking/{id}")
    fun getMyBooking(@PathVariable id:String):List<RunSession> {
        return sessionControllersProvider.getMyBookingSessionController.execute(id)
    }

    @GetMapping("/list")
    fun getSessionList( @RequestParam(required = true) long: Double,
                        @RequestParam(required = true) lat: Double,
                        @RequestParam("date")
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ):List<RunSession> {
        return sessionControllersProvider.getRunSessionListController.execute(
            com.example.runitup.web.rest.v1.dto.SessionListModel(
                longitude = long,
                latitude = lat,
                date = date
            )
        )
    }

    @PostMapping("/check-in")
    fun checkInLevel(@RequestBody model: com.example.runitup.web.rest.v1.dto.user.CheckIn): RunSession {
        return sessionControllersProvider.checkInController.execute(model)
    }

    @PostMapping("/cancel")
    fun checkInLevel(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.CancelSessionModel): RunSession {
        return sessionControllersProvider.cancelSessionController.execute(model)
    }

    @PostMapping("/complete")
    fun completeSession(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel): RunSession {
        return sessionControllersProvider.completeSessionController.execute(model)
    }

    @PostMapping("/leave")
    fun leaveSession(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.CancelSessionModel): RunSession {
        return sessionControllersProvider.leaveSessionController.execute(model)
    }

    @PostMapping("/start")
    fun startSession(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.ConfirmSessionModel): RunSession {
        return sessionControllersProvider.startSessionController.execute(model)
    }

    @PostMapping("/update/guest")
    fun updateSessionGuest(@RequestBody model: com.example.runitup.web.rest.v1.dto.session.JoinSessionModel): RunSession {
        return sessionControllersProvider.updateSessionGuest.execute(model)
    }

    @PostMapping("/payment/intent")
    fun createPaymentIntent(@RequestBody model: com.example.runitup.web.rest.v1.dto.stripe.CreatePIRequest): com.example.runitup.web.rest.v1.dto.stripe.CreatePIResponse {
        return sessionControllersProvider.createPaymentIntent.execute(model)
    }
}