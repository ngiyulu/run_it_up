package com.example.runitup.web.rest.v1.controllers.user.update

import com.example.runitup.exception.ApiRequestException
import com.example.runitup.model.User
import com.example.runitup.security.UserPrincipal
import com.example.runitup.web.rest.v1.controllers.BaseController
import com.example.runitup.web.rest.v1.dto.user.UpdateDob
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UpdateDobController: BaseController<UpdateDob, User>() {
    override fun execute(request: com.example.runitup.web.rest.v1.dto.user.UpdateDob): User {
        val auth = SecurityContextHolder.getContext().authentication
        val savedUser = auth.principal as UserPrincipal
        var user = cacheManager.getUser(savedUser.id.toString()) ?: throw ApiRequestException(text("user_not_found"))
        user.dob = request.dob
        user = cacheManager.updateUser(user)
        return user
    }
}