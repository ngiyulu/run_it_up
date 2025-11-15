package com.example.runitup.mobile.rest.v1.controllers.user.update

import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.UpdateDob
import org.springframework.stereotype.Service

@Service
class UpdateDobController: BaseController<UpdateDob, User>() {
    override fun execute(request: UpdateDob): User {
        var user = getMyUser()
        user.dob = request.dob
        user = cacheManager.updateUser(user)
        return user
    }
}