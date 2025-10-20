package com.example.runitup.mobile.rest.v1.controllers.user.update

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.UpdateEmailModel
import com.example.runitup.mobile.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateEmailController: BaseController<UpdateEmailModel, User>() {
    override fun execute(request: UpdateEmailModel): User {
        val auth =  SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        var user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        user.email = request.email
        user = cacheManager.updateUser(user)
        return user
    }
}