package com.example.runitup.web.rest.v1.restcontroller

import com.example.runitup.model.Gym
import com.example.runitup.web.rest.v1.controllerprovider.GymControllersProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/gym")
@RestController
class GymRestController {

    @Autowired
    lateinit var gymControllersProvider: GymControllersProvider

    @PostMapping("/create")
    fun create(@RequestBody model: com.example.runitup.web.rest.v1.dto.CreateGymRequest): Gym {
        return gymControllersProvider.createGymController.execute(model)
    }

}