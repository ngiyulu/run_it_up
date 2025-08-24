package com.example.runitup.web.rest.v1.controllers.user.update

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.user.UpdateNameModel
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateNameController: BaseController<UpdateNameModel, User>() {
    override fun execute(request: com.example.runitup.web.rest.v1.dto.user.UpdateNameModel): User {
        val auth =  SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        var user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        user.firstName = request.firstName
        user.lastName = request.lastName
        user = cacheManager.updateUser(user)
        return user
    }
}