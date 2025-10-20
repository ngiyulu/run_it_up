package com.example.runitup.mobile.rest.v1.controllers.user

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.BlockUser
import org.springframework.stereotype.Service

@Service
class BlockUserController: BaseController<BlockUser, User>() {
    override fun execute(request: BlockUser): User {
        var user = cacheManager.getUser(request.userId) ?: throw ApiRequestException(text("user_not_found"))
        user.isActive = false
        user = cacheManager.updateUser(user)
        return user
    }
}