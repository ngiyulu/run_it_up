package com.example.runitup.mobile.rest.v1.restcontroller

import com.example.runitup.mobile.model.AccountDeletionRequest
import com.example.runitup.mobile.rest.v1.controllers.accountdeletion.CreateAccountDeletionController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/acct-delete")
class AccountDeletionRest {

    @Autowired
    lateinit var createAccountDeletionController: CreateAccountDeletionController

    @PostMapping("/create")
    fun create(): AccountDeletionRequest{
        return createAccountDeletionController.execute(Unit)
    }

}