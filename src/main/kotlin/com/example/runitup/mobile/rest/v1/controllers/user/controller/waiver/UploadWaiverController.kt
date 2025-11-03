package com.example.runitup.mobile.rest.v1.controllers.user.controller.waiver

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.utility.AgeUtil
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UploadWaiverController: BaseController<Pair<String, FileUploadModel>, User>() {

    @Autowired
    lateinit var service: GcsImageService

    @Autowired
    protected lateinit var manager: MyCacheManager

    @Autowired
    lateinit var waiverRepository: WaiverRepository
    override fun execute(request: Pair<String, FileUploadModel>): User {
        val(zoneId, file) = request
        val user = manager.getUser(file.data.orEmpty()) ?: throw ApiRequestException(text("user_not_found"))
        val age = AgeUtil.ageFrom(user.dob, zoneIdString = zoneId)
        val auth =  SecurityContextHolder.getContext().authentication
        val savedAdmin = auth.principal as AdminPrincipal
        print("age = $age")
        if(age >18 || !user.waiverUrl.isNullOrEmpty()){
            throw ApiRequestException("invalid_request")
        }
        val waiver = waiverRepository.findByUserId(user.id.orEmpty()) ?: throw ApiRequestException("invalid_request")

        val waiverUrl = service.uploadUserWaiverPdf(user, file.file) ?: throw ApiRequestException("error")
        user.approveWaiver(savedAdmin.admin.id.orEmpty(), getTimeStamp(), waiverUrl.url, true)
        waiver.approve(savedAdmin.admin.id.orEmpty(), true, "")
        waiver.approvedAt = LocalDate.now()
        waiver.approvedBy = savedAdmin.admin.id
        waiver.status = WaiverStatus.APPROVED
        waiverRepository.save(waiver)

        return cacheManager.updateUser(user)
    }

}