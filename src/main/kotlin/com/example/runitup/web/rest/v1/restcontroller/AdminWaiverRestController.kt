package com.example.runitup.web.rest.v1.restcontroller


import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import com.example.runitup.mobile.service.TextService
import com.example.runitup.web.rest.v1.controller.waiver.ApproveWaiverController
import com.example.runitup.web.rest.v1.controller.waiver.ApproveWaiverModel
import com.example.runitup.web.rest.v1.controller.waiver.UploadWaiverController
import com.example.runitup.web.rest.v1.controller.waiver.WaiverListController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/admin/api/v1/waiver")
class AdminWaiverRestController {


    @Autowired
    lateinit var uploadWaiverController: UploadWaiverController

    @Autowired
    lateinit var waiverListController: WaiverListController

    @Autowired
    lateinit var approveWaiverController: ApproveWaiverController

    @Autowired
    lateinit var textService: TextService

    @GetMapping("/list")
    fun getWaiverList(): List<Waiver> {
       return  waiverListController.execute(Unit)
    }

    @PostMapping("/approve")
    fun getWaiverList(@RequestBody request: ApproveWaiverModel): Waiver {
        return  approveWaiverController.execute(request)
    }

}