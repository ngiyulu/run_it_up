package com.example.runitup.mobile.rest.v1.controllers.user.update

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.UpdateEmailModel
import org.springframework.stereotype.Service

@Service
class UpdateEmailController: BaseController<UpdateEmailModel, User>() {
    override fun execute(request: UpdateEmailModel): User {
        var user = getMyUser()
        user.email = request.email
        user = cacheManager.updateUser(user)
        return user
    }
}