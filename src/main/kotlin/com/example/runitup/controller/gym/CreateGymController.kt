package com.example.runitup.controller.gym

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.CreateGymRequest
import com.example.runitup.model.Gym
import com.example.runitup.repository.GymRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.stereotype.Service

@Service
class CreateGymController: BaseController<CreateGymRequest, Gym>() {

    @Autowired
    lateinit var gymRepository: GymRepository
    override fun execute(request: CreateGymRequest): Gym {
        val gym = request.gym
        gym.location = GeoJsonPoint(request.longitude, request.latitude) // lon, lat
        gym.createdAt = getTimeStamp()
        gymRepository.save(gym)
        return gymRepository.save(gym)
    }
}