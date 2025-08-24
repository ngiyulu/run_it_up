package com.example.runitup.web.rest.v1.controllers.gym

import com.example.runitup.model.Gym
import com.example.runitup.repository.GymRepository
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.CreateGymRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.stereotype.Service

@Service
class CreateGymController: BaseController<CreateGymRequest, Gym>() {

    @Autowired
    lateinit var gymRepository: GymRepository
    override fun execute(request: com.example.runitup.web.rest.v1.dto.CreateGymRequest): Gym {
        val gym = request.gym
        gym.location = GeoJsonPoint(request.longitude, request.latitude) // lon, lat
        gym.createdAt = getTimeStamp()
        gymRepository.save(gym)
        return gymRepository.save(gym)
    }
}