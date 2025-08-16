package com.example.runitup.restcontroller

import com.example.runitup.controllerprovider.GymControllersProvider
import com.example.runitup.dto.CreateGymRequest
import com.example.runitup.model.Gym
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.geo.Distance
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.awt.Point

@RequestMapping("/api/v1/gym")
@RestController
class GymRestController {

    @Autowired
    lateinit var gymControllersProvider: GymControllersProvider

    @PostMapping("/create")
    fun create(@RequestBody model: CreateGymRequest): Gym {
        return gymControllersProvider.createGymController.execute(model)
    }

}