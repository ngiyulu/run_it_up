package com.example.runitup.web.rest.v1.controller

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivateUserController : BaseController<ActivateUserModel, User>() {


    override fun execute(request: ActivateUserModel): User {
        val admin = getMyAdmin()
        val user = cacheManager.getUser(request.userId)?: throw ApiRequestException("user_not_found")
        user.isActive = request.activate
        user.updatedActivatedStatusAt = Instant.now()
        user.updatedActivatedStatusBy = admin.id
        cacheManager.updateUser(user)
        return  user
    }
}

data class ActivateUserModel(val userId:String, val activate:Boolean)