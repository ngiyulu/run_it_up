package com.example.runitup.controller.gym

import com.example.runitup.controller.BaseController
import com.example.runitup.model.Gym
import com.example.runitup.repository.GymRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateGymController: BaseController<Gym, Gym>() {

    @Autowired
    lateinit var gymRepository: GymRepository
    override fun execute(request: Gym): Gym {
        request.createdAt = getTimeStamp()
        gymRepository.save(request)
        return  gymRepository.save(request)
    }
}