package com.example.runitup.web.rest.v1.controllerprovider

import com.example.runitup.web.rest.v1.controllers.gym.CreateGymController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GymControllersProvider {
    @Autowired
    lateinit var createGymController: CreateGymController
}