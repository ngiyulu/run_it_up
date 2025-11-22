package com.example.runitup.mobile.rest.v1.controllers.runsession

import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.service.PromotionResult
import com.example.runitup.mobile.service.PromotionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PromoteUserController: BaseController<PromotionRequestModel, PromotionResult>() {

    @Autowired
    lateinit var promotionService: PromotionService
    override fun execute(request: PromotionRequestModel): PromotionResult {
        return  promotionService.promoteNextWaitlistedUser(request.sessionId)
    }
}

data class PromotionRequestModel(val sessionId:String)