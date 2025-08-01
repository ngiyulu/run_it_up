package com.example.runitup.controller.user.update

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.user.UpdateSkillLevel
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.extensions.mapFromString
import com.example.runitup.model.User
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateSkillLevelController: BaseController<UpdateSkillLevel, User>() {
    override fun execute(request: UpdateSkillLevel): User {
        val auth =  SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as User
        var user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        user.skillLevel = request.skillLevel.mapFromString()
        user = cacheManager.updateUser(user)
        return user
    }
}