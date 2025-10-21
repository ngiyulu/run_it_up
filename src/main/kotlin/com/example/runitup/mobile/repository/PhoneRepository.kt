package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.enum.PhoneType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PHONE_COLLECTION)
interface PhoneRepository : MongoRepository<com.example.runitup.mobile.model.Phone, String> {

    @Query("{phoneId:'?0'}")
    fun findByPhoneId(phoneId: String): com.example.runitup.mobile.model.Phone?

    /**
     * Find all phones registered under a given logical phoneId.
     * This can represent multiple installs of the same device or user.
     */
    fun findAllByPhoneId(phoneId: String): List<com.example.runitup.mobile.model.Phone>

    /**
     * Find all phones of a specific platform (ANDROID or IOS).
     */
    fun findAllByType(type: PhoneType): List<com.example.runitup.mobile.model.Phone>

    /**
     * Find all phones that match a list of push tokens.
     */
    fun findAllByTokenIn(tokens: Collection<String>): List<com.example.runitup.mobile.model.Phone>

    /**
     * Find all phones belonging to a list of IDs.
     */
    fun findAllByIdIn(ids: Collection<String>): List<com.example.runitup.mobile.model.Phone>

    fun findAllByUserId(userId: String): List<com.example.runitup.mobile.model.Phone>

    fun findAllByUserIdIn(userIds: Collection<String>): List<com.example.runitup.mobile.model.Phone>
}