package com.example.runitup.mobile.rest.v1.controllers.user.controller.gym

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.dto.CreateGymRequest
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.service.GeocodingService
import com.example.runitup.mobile.service.ImageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.stereotype.Service

@Service
class UpdateGymController: BaseGymController() {

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var gmsService: GcsImageService

    @Autowired
    lateinit var imageService: ImageService

    @Autowired
    lateinit var  geocodingService: GeocodingService

    override fun execute(request: CreateGymRequest): Gym {
        val data = parseCreate(request.payloadRaw)
        val ad = data.address
        val gymDb =   gymRepository.findById(request.id)
        if(!gymDb.isPresent){
            throw ApiRequestException(text(text("gym_not_found")))
        }
        val gym = gymDb.get()
        request.image?.let {
            val resized = imageService.resizeToAvatarJpeg(it, maxSize = 512, quality = 0.85)
            val uploadResponse: GcsImageService.UploadResult = gmsService.uploadGymImage(gym, resized)
                ?: throw ApiRequestException(text("image_upload_failure"))
            gym.image = uploadResponse.url
        }
        gym.title = data.title
        gym.fee = data.fee
        gym.notes = data.notes
        gym.description = data.description
        gym.updatedAt = getTimeStamp()

        ad?.let {
            gym.line1 = it.line1
            gym.line2 = it.line2
            gym.zipCode = it.zip
            gym.city = it.city
            gym.state = it.state
        }
        val location = geocodingService.geocode(gym.getAddress())
        gym.location = GeoJsonPoint(location.latitude, location.longitude)
        return gymRepository.save(gym)
    }


}
