package com.example.runitup.restcontroller

import com.example.runitup.controllerprovider.SessionControllersProvider
import com.example.runitup.dto.CreateRunSessionRequest
import com.example.runitup.dto.SessionListModel
import com.example.runitup.dto.session.CancelSessionModel
import com.example.runitup.dto.session.ConfirmSessionModel
import com.example.runitup.dto.session.JoinSessionModel
import com.example.runitup.dto.user.CheckIn
import com.example.runitup.model.RunSession
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
    fun create(@RequestBody model: CreateRunSessionRequest):RunSession {
        return sessionControllersProvider.createSessionController.execute(model)
    }

    @PostMapping("/join")
    fun join(@RequestBody model: JoinSessionModel):RunSession {
        return sessionControllersProvider.joinSessionController.execute(model)
    }

    @PostMapping("/waitlist")
    fun joinWaitList(@RequestBody model: JoinSessionModel):RunSession {
        return sessionControllersProvider.joinWaitListController.execute(model)
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody model: ConfirmSessionModel):RunSession {
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
        return sessionControllersProvider.getRunSessionListController.execute(SessionListModel(longitude = long, latitude = lat, date = date))
    }

    @PostMapping("/check-in")
    fun checkInLevel(@RequestBody model: CheckIn): RunSession {
        return sessionControllersProvider.checkInController.execute(model)
    }

    @PostMapping("/cancel")
    fun checkInLevel(@RequestBody model: CancelSessionModel): RunSession {
        return sessionControllersProvider.cancelSessionController.execute(model)
    }

    @PostMapping("/complete")
    fun completeSession(@RequestBody model: ConfirmSessionModel): RunSession {
        return sessionControllersProvider.completeSessionController.execute(model)
    }

    @PostMapping("/leave")
    fun leaveSession(@RequestBody model: CancelSessionModel): RunSession {
        return sessionControllersProvider.leaveSessionController.execute(model)
    }

    @PostMapping("/start")
    fun startSession(@RequestBody model: ConfirmSessionModel): RunSession {
        return sessionControllersProvider.startSessionController.execute(model)
    }

    @PostMapping("/update/guest")
    fun updateSessionGuest(@RequestBody model: JoinSessionModel): RunSession {
        return sessionControllersProvider.updateSessionGuest.execute(model)
    }
}