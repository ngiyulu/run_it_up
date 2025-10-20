package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.file.GymImageUploader
import com.example.runitup.mobile.rest.v1.controllers.file.ProfileImageUploader
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import com.example.runitup.mobile.service.GcsImageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/v1/file")
class FileRestController{

    @Autowired
    lateinit var gcsImageService: GcsImageService

    @Autowired
    lateinit var profileImageUploader: ProfileImageUploader
    @Autowired
    lateinit var gymImageUploader: GymImageUploader

    @PostMapping("/upload/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile
    ): User {
        return profileImageUploader.execute(FileUploadModel(file, null))
    }

    @PostMapping("/upload/gym-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadGymImage(
        @RequestPart("file") file: MultipartFile,
        @RequestPart("gymId") gymId: String
    ): com.example.runitup.mobile.model.Gym {
        return gymImageUploader.execute(FileUploadModel(file, gymId))
    }

    // Use a query param for objectName to avoid issues with slashes in @PathVariable
    @DeleteMapping
    fun delete(@RequestParam objectName: String): ResponseEntity<Void> =
        if (gcsImageService.delete(objectName)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}