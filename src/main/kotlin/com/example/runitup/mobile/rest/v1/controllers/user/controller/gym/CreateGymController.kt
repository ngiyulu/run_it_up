package com.example.runitup.mobile.rest.v1.controllers.user.controller.gym

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.dto.CreateGymRequest
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.service.ImageService
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CreateGymController: BaseGymController() {

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var gmsService: GcsImageService

    @Autowired
    lateinit var imageService: ImageService

    override fun execute(request: CreateGymRequest): Gym {
        val data = parseCreate(request.payloadRaw)
        val ad = data.address
        val gym =   Gym(
            id = ObjectId().toString(),
            line1 = ad?.line1.orEmpty(),
            line2 = ad?.line2.orEmpty(),
            location = GeoJsonPoint(-83.749112, 42.488622),
            image = null,
            fee = data.fee,
            phoneNumber = data.phoneNumber,
            city = ad?.city.orEmpty(),
            title = data.title,
            state = ad?.state.orEmpty(),
            notes = data.notes,
            description = data.description,
            zipCode = ad?.zip.orEmpty())
        gym.createdAt = LocalDate.now()

        // i need to figure out how to get the long and lat of the gym
        val resized = imageService.resizeToAvatarJpeg(request.image!!, maxSize = 512, quality = 0.85)
        val uploadResponse: GcsImageService.UploadResult = gmsService.uploadGymImage(gym, resized)
            ?: throw ApiRequestException(text("image_upload_failure"))
        gym.image = uploadResponse.url
        return gymRepository.save(gym)
    }


}
