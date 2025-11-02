package com.example.runitup.mobile.rest.v1.controllers.waiver

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.FileUploadModel
import com.example.runitup.mobile.security.UserPrincipal
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.utility.AgeUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CreateWaiverController: BaseController<CreateWaiverRequest, Waiver>() {

    @Autowired
    lateinit var service: GcsImageService

    @Autowired
    lateinit var manager: MyCacheManager

    @Autowired
    lateinit var waiverRepository: WaiverRepository
    override fun execute(request: CreateWaiverRequest): Waiver {
        val user = manager.getUser(request.userId) ?: throw ApiRequestException(text("user_not_found"))
        val age = AgeUtil.ageFrom(user.dob, zoneIdString = request.zoneId)
        print("age = $age")
        if(age >18){
            throw ApiRequestException("invalid_request")
        }
        val waiver = waiverRepository.findByUserId(request.userId)
        // this means we are resumbitting
        if(waiver != null){
            if(waiver.status == WaiverStatus.APPROVED){
                throw ApiRequestException("waiver_approved")
            }
            val waiverUrl = service.uploadUserWaiverPdf(user, request.fileUploadModel.file) ?: throw ApiRequestException("error")
            waiver.url = waiverUrl.url
            waiver.updatedAt = getTimeStamp()
            waiver.approvedAt = null
            waiver.approvedBy = null
            waiver.status = WaiverStatus.PENDING
            waiver.note = ""
            return waiverRepository.save(waiver)
        }
        val waiverUrl = service.uploadUserWaiverPdf(user, request.fileUploadModel.file) ?: throw ApiRequestException("error")
        // this means we are creating a new submittion
        return waiverRepository.save(Waiver(
            userId = request.userId,
            approvedAt = null,
            url = waiverUrl.url
        ))
    }

}

class CreateWaiverRequest(val fileUploadModel: FileUploadModel, val zoneId:String, val userId:String)