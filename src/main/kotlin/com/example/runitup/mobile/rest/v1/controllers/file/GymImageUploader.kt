package com.example.runitup.mobile.rest.v1.controllers.file

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Gym
import com.example.runitup.mobile.repository.GymRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.service.ImageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GymImageUploader: BaseController<FileUploadModel, Gym>() {

    @Autowired
    lateinit var service: GcsImageService

    @Autowired
    lateinit var gymRepository: GymRepository

    @Autowired
    lateinit var imageService: ImageService

    override fun execute(request: FileUploadModel): com.example.runitup.mobile.model.Gym {
        val resized = imageService.resizeToAvatarJpeg(request.file, maxSize = 512, quality = 0.85)
        val gymRes = gymRepository.findById(request.gymId.orEmpty())
        if(!gymRes.isPresent){
            throw ApiRequestException(text("gym_not_found"))
        }
        val gym = gymRes.get()
        val storageResponse = service.uploadGymImage(gym, resized) ?: throw ApiRequestException("error")
        gym.image = storageResponse.url

        return gymRepository.save(gym)
    }

}