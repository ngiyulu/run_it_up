package com.example.runitup.mobile.rest.v1.controllers.user.controller.waiver

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.controllers.UserModelType
import com.example.runitup.mobile.service.GcsImageService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.web.security.AdminPrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class ApproveWaiverController: BaseController<ApproveWaiverModel, Waiver>() {

    @Autowired
    lateinit var service: GcsImageService

    @Autowired
    protected lateinit var maanager: MyCacheManager

    @Autowired
    lateinit var waiverRepository: WaiverRepository

    @Autowired
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var queueService: LightSqsService
    override fun execute(request: ApproveWaiverModel): Waiver {
        var myUser = getUser()
        val waiverDb = waiverRepository.findById(request.waiverId)
        if(!waiverDb.isPresent){
            throw ApiRequestException("invalid_request")
        }
        val waiver = waiverDb.get()
        val user = maanager.getUser(waiver.userId) ?: throw ApiRequestException(text("user_not_found"))
        var admin = if(myUser.type == UserModelType.ADMIN){
            myUser.adminUser!!
        } else{
            val u = myUser.user!!
            if( u.linkedAdmin == null){
                throw ApiRequestException("unauthorized_user")
            }
            cacheManager.getAdmin(u.linkedAdmin.orEmpty())
        }
        if(admin == null){
            throw ApiRequestException("unauthorized_user")
        }
        // no need to take action because the waiver is already processed
        if(waiver.status == WaiverStatus.APPROVED){
            return  waiver
        }
        user.approveWaiver(admin.id.orEmpty(), waiver.url, request.isApproved)
        cacheManager.updateUser(user)
        waiver.approve(admin.id.orEmpty(), request.isApproved, request.notes)
        appScope.launch {
            val data = JobEnvelope(
                jobId = UUID.randomUUID().toString(),
                taskType = "New user, waiver approved",
                payload = user.id.orEmpty()
            )
            queueService.sendJob(QueueNames.NEW_USER_JOB, data,  delaySeconds = 0)
        }
        return waiverRepository.save(waiver)
    }

}

class ApproveWaiverModel(val waiverId:String, val isApproved:Boolean, val notes:String)