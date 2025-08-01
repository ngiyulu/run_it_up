package com.example.runitup.controller.user

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.user.BlockUser
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
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