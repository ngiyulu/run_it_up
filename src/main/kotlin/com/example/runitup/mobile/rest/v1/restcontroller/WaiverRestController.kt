package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.rest.v1.controllers.waiver.CreateWaiverController
import com.example.runitup.mobile.rest.v1.controllers.waiver.CreateWaiverRequest
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/v1/waiver")
@RestController
class WaiverRestController {

    @Autowired
    lateinit var waiverController: CreateWaiverController
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadWaiver(
        @RequestPart("file") file: MultipartFile,
        @RequestHeader("X-Timezone", required = true) tzHeader: String,
    ): Waiver {
        return waiverController.execute(
            CreateWaiverRequest(FileUploadModel(file, null), tzHeader)
        )
    }

}