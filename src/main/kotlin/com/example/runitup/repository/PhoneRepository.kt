package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
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
}