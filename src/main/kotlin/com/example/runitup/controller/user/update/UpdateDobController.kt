package com.example.runitup.controller.user.update

import com.example.runitup.controller.BaseController
import com.example.runitup.dto.user.UpdateDob
import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateDobController: BaseController<UpdateDob, User>() {
    override fun execute(request: UpdateDob): User {
        val auth = SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        var user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        user.dob = request.dob
        user = cacheManager.updateUser(user)
        return user
    }
}