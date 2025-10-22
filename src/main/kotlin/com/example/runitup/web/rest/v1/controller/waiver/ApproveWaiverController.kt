package com.example.runitup.web.rest.v1.controller.waiver

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.web.security.AdminPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ApproveWaiverController: BaseController<ApproveWaiverModel, Waiver>() {

    @Autowired
    lateinit var service: GcsImageService

    @Autowired
    protected lateinit var maanager: MyCacheManager

    @Autowired
    lateinit var waiverRepository: WaiverRepository
    override fun execute(request: ApproveWaiverModel): Waiver {
        val user = maanager.getUser(request.userId) ?: throw ApiRequestException(text("user_not_found"))
        val auth =  SecurityContextHolder.getContext().authentication
        val savedAdmin = auth.principal as AdminPrincipal
        val waiverDb = waiverRepository.findById(request.waiverId)
        if(!waiverDb.isPresent){
            throw ApiRequestException("invalid_request")
        }
        val waiver = waiverDb.get()
        // no need to take action because the waiver is already procesesed
        if(waiver.approvedAt != null){
            return  waiver
        }
        user.approveWaiver(savedAdmin.admin.id.orEmpty(), getTimeStamp(), null)
        cacheManager.updateUser(user)
        waiver.approve(savedAdmin.admin.id.orEmpty(), request.isApproved, request.notes)
        return waiverRepository.save(waiver)
    }

}

class ApproveWaiverModel(val waiverId:String, val isApproved:Boolean, val notes:String, val userId: String)