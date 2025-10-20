package com.example.runitup.mobile.rest.v1.controllers.file

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.service.ImageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ProfileImageUploader: BaseController<FileUploadModel, User>() {

    @Autowired
    lateinit var service: GcsImageService

    @Autowired
    lateinit var imageService: ImageService

    @Autowired
    protected lateinit var maanager: MyCacheManager
    override fun execute(request: FileUploadModel): User {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = maanager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val resized = imageService.resizeToAvatarJpeg(request.file, maxSize = 512, quality = 0.85)
        val storageResponse = service.uploadProfileImage(user, resized) ?: throw ApiRequestException("error")
        user.imageUrl = storageResponse.url
        return cacheManager.updateUser(user)
    }

}