package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.enum.PhoneType
import com.example.runitup.model.Phone
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PHONE_COLLECTION)
interface PhoneRepository : MongoRepository<Phone, String> {

    @Query("{phoneId:'?0'}")
    fun findByPhoneId(phoneId: String): Phone?

    /**
     * Find all phones registered under a given logical phoneId.
     * This can represent multiple installs of the same device or user.
     */
    fun findAllByPhoneId(phoneId: String): List<Phone>

    /**
     * Find all phones of a specific platform (ANDROID or IOS).
     */
    fun findAllByType(type: PhoneType): List<Phone>

    /**
     * Find all phones that match a list of push tokens.
     */
    fun findAllByTokenIn(tokens: Collection<String>): List<Phone>

    /**
     * Find all phones belonging to a list of IDs.
     */
    fun findAllByIdIn(ids: Collection<String>): List<Phone>
}