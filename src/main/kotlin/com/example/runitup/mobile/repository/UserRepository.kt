package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.USER_COLLECTION)
interface UserRepository : MongoRepository<User, String> {
    @Query("{id:'?0'}")
    fun findByIdentifier(identifier: String): User?
    @Query("{email:'?0'}")
    fun findByEmail(email: String): User?

    fun findByLinkedAdmin(linkedAdmin: String): User?

    @Query("{phoneNumber:'?0'}")
    fun findByPhone(phoneNumber: String): User?

    @Query("{auth:'?0'}")
    fun findByAuth(auth: String): User?
    fun findAllByVerifiedPhone(verifiedPhone: Boolean, pageable: Pageable): Page<User>
}