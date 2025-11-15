package com.example.runitup.mobile.rest.v1.controllers.user.update

import com.example.runitup.mobile.extensions.mapFromString
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import com.example.runitup.mobile.rest.v1.dto.user.UpdateSkillLevel
import org.springframework.stereotype.Service

@Service
class UpdateSkillLevelController: BaseController<UpdateSkillLevel, User>() {
    override fun execute(request: UpdateSkillLevel): User {
        var user = getMyUser()
        user.skillLevel = request.skillLevel.mapFromString()
        user = cacheManager.updateUser(user)
        return user
    }
}