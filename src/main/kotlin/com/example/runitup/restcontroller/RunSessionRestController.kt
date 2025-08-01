package com.example.runitup.restcontroller

import com.example.runitup.controllerprovider.SessionControllersProvider
import com.example.runitup.dto.session.CancelSessionModel
import com.example.runitup.dto.session.ConfirmSessionModel
import com.example.runitup.dto.session.JoinSessionModel
import com.example.runitup.dto.user.CheckIn
import com.example.runitup.model.RunSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/run-session")
@RestController
class RunSessionRestController {

    @Autowired
    lateinit var sessionControllersProvider: SessionControllersProvider
    @PostMapping("/create")
    fun create(@RequestBody model: RunSession):RunSession {
        return sessionControllersProvider.createSessionController.execute(model)
    }

    @PostMapping("/join")
    fun join(@RequestBody model: JoinSessionModel):RunSession {
        return sessionControllersProvider.joinSessionController.execute(model)
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

    @PostMapping("/update/guess")
    fun updateSessionGuest(@RequestBody model: JoinSessionModel): RunSession {
        return sessionControllersProvider.updateSessionGuest.execute(model)
    }
}