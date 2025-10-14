package com.example.runitup.web.rest.v1.controllers.file

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.Gym
import com.example.runitup.repository.GymRepository
import com.example.runitup.service.GcsImageService
import com.example.runitup.service.ImageService
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.FileUploadModel
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

    override fun execute(request: FileUploadModel): Gym {
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