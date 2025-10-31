package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Support
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
@Document(collection = CollectionConstants.SUPPORT_COLLECTION)
interface SupportRepository : MongoRepository<Support, String>{
    fun findByStatusAndCreatedAt(status:String, createdAt:LocalDate): List<Support>

    fun findByStatus(status:String): List<Support>
}