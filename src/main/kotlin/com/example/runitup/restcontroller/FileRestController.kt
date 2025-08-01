//package com.example.runitup.restcontroller
//
//import GcsImageService
//import GcsImageService.UploadResult
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.http.MediaType
//import org.springframework.http.ResponseEntity
//import org.springframework.web.bind.annotation.*
//import org.springframework.web.multipart.MultipartFile
//
//
//@RestController
//@RequestMapping("/api/files")
//class FileRestController{
//
//    @Autowired
//    lateinit var gcsImageService: GcsImageService
//    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
//    fun uploadImage(
//        @RequestPart("file") file: MultipartFile,
//        @RequestParam(defaultValue = "true") signed: Boolean,
//        @RequestParam(defaultValue = "15") ttlMinutes: Long
//    ): ResponseEntity<UploadResult> =
//        ResponseEntity.ok(gcsImageService.upload(file, signed, ttlMinutes))
//
//    // Use a query param for objectName to avoid issues with slashes in @PathVariable
//    @DeleteMapping
//    fun delete(@RequestParam objectName: String): ResponseEntity<Void> =
//        if (gcsImageService.delete(objectName)) ResponseEntity.noContent().build()
//        else ResponseEntity.notFound().build()
//}