package com.example.runitup.web.rest.v1.controller.waiver

import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.model.WaiverStatus
import com.example.runitup.mobile.repository.WaiverRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WaiverListController: BaseController<Unit, List<Waiver>>() {

    @Autowired
    lateinit var waiverRepository: WaiverRepository
    override fun execute(request: Unit): List<Waiver> {
        val waiverList =  waiverRepository.findWaiverByStatus(WaiverStatus.PENDING)
        return waiverList.map {
            it.user = cacheManager.getUser(it.userId)
            it
        }
    }

}