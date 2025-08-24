package com.example.runitup.web.rest.v2.restcontroller

import com.example.runitup.web.rest.v2.controllerprovider.SessionControllersV2Provider
import com.example.runitup.web.rest.v2.dto.MyBookingModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v2/run-session")
@RestController
class RunSessionV2RestController {
    @Autowired
    lateinit var sessionControllersV2Provider: SessionControllersV2Provider


    @GetMapping("/booking/{id}")
    fun getMyBooking(@PathVariable id:String):List<MyBookingModel> {
        return sessionControllersV2Provider.getMyBookingSessionController.execute(id)
    }
}