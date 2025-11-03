package com.example.runitup.mobile.rest.v1.controllers.user.controller.gym

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GetGym: BaseController<String, Gym>() {

    @Autowired
    lateinit var gymRepository: GymRepository
    override fun execute(request: String): Gym {
        val gymDb = gymRepository.findById(request)
        if(!gymDb.isPresent) {
            throw ApiRequestException("gym_not_found")
        }
        return gymDb.get()
    }
}