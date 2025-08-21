package com.example.runitup.restcontroller

import com.example.runitup.controller.file.ProfileImageUploader
import com.example.runitup.dto.FileUploadModel
import com.example.runitup.model.User
import com.example.runitup.service.GcsImageService
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

    @PostMapping("/upload/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile
    ): User {
        return profileImageUploader.execute(FileUploadModel(file))
    }

    // Use a query param for objectName to avoid issues with slashes in @PathVariable
    @DeleteMapping
    fun delete(@RequestParam objectName: String): ResponseEntity<Void> =
        if (gcsImageService.delete(objectName)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
}