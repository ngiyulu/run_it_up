package com.example.runitup.controllerprovider

import com.example.runitup.controller.gym.CreateGymController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GymControllersProvider {
    @Autowired
    lateinit var createGymController: CreateGymController
}