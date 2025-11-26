package com.example.runitup.mobile.rest.v1.controllers.accountdeletion

import com.example.runitup.mobile.model.AccountDeletionRequest
import com.example.runitup.mobile.repository.AccountDeletionRepository
import com.example.runitup.mobile.rest.v1.controllers.BaseController
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateAccountDeletionController: BaseController<Unit, AccountDeletionRequest>() {

    @Autowired
    lateinit var accountDeletionRepository: AccountDeletionRepository
    override fun execute(request: Unit): AccountDeletionRequest {
        val user = getMyUser()
        return accountDeletionRepository.save(
            AccountDeletionRequest(ObjectId().toString(), user.id.orEmpty())
        )
    }
}

