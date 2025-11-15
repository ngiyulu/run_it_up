package com.example.runitup.mobile.rest.v1.controllers.user.update

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.UpdateNameModel
import org.springframework.stereotype.Service

@Service
class UpdateNameController: BaseController<UpdateNameModel, User>() {
    override fun execute(request: UpdateNameModel): User {
        var user = getMyUser()
        user.firstName = request.firstName
        user.lastName = request.lastName
        user = cacheManager.updateUser(user)
        return user
    }
}