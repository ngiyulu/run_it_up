package com.example.runitup.mobile.rest.v1.controllers.user.controller.gym

import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetGymList: BaseController<Unit, List<Gym>>() {

    @Autowired
    lateinit var gymRepository: GymRepository


    override fun execute(request: Unit): List<Gym> {
        return gymRepository.findAll().filter { it.isActive }.sortedBy { it.title.lowercase() }
    }
}