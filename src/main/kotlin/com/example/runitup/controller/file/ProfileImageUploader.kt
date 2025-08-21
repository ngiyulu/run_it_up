package com.example.runitup.controller.file

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.FileUploadModel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.security.UserPrincipal
import com.example.runitup.service.GcsImageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ProfileImageUploader: BaseController<FileUploadModel, User>() {

    @Autowired
    lateinit var service: GcsImageService
    override fun execute(request: FileUploadModel): User {
        val auth = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = cacheManager.getUser(auth.id.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val storageResponse = service.upload(request.file) ?: throw ApiRequestException("error")
        user.imageUrl = storageResponse.url
        return cacheManager.updateUser(user)
    }

}