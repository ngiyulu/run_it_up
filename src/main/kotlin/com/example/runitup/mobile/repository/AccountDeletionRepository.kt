package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.AccountDeletionRequest
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.ACCOUNT_DELETION)
interface AccountDeletionRepository : MongoRepository<AccountDeletionRequest, String> {

}