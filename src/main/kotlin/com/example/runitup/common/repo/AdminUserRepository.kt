package com.example.runitup.common.repo

import com.example.runitup.common.model.AdminUser
import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.User
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.ADMIN_COLLECTION)
interface AdminUserRepository : MongoRepository<AdminUser, String> {
    fun findByEmail(email: String): AdminUser?

    @Query("{id:'?0'}")
    fun findByIdentifier(identifier: String): AdminUser?
}