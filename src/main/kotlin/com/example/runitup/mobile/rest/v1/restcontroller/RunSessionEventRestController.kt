package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.rest.v1.controllers.runsessionevent.GetRunSessionEvent
import com.example.runitup.mobile.rest.v1.controllers.support.CreateSupportController
import com.example.runitup.mobile.rest.v1.dto.CreateSupportRequest
import com.example.runitup.mobile.rest.v1.dto.RunSessionEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/run-session/event")
@RestController
class RunSessionEventRestController {

    @Autowired
    lateinit var getRunSessionEvent: GetRunSessionEvent

    @GetMapping("/list/{sessionId}")
    fun getList(@PathVariable sessionId: String): List<RunSessionEvent> {
        return getRunSessionEvent.execute(sessionId)
    }



}