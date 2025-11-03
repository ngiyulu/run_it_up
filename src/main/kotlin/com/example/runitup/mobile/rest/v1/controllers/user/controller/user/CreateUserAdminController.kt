package com.example.runitup.mobile.rest.v1.controllers.user.controller.user

import com.example.runitup.mobile.model.Creator
import com.example.runitup.mobile.model.User
import com.example.runitup.mobile.rest.v1.controllers.user.CreateUserController
import org.springframework.stereotype.Service

@Service
class CreateUserAdminController: CreateUserController() {

    override fun userProvided(user: User) {
        super.userProvided(user)
        user.verifiedPhone = true
        user.creator = Creator.ADMIN
    }
}