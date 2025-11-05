package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.rest.v1.controllers.user.controller.gym.GetGymList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/gym")
@RestController
class GymRestController {

    @Autowired
    lateinit var getGymList: GetGymList

    @GetMapping("/list")
    fun getGymList(): List<Gym> {
        return getGymList.execute(Unit)
    }

}