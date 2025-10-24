package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Waiver
import com.example.runitup.mobile.model.WaiverStatus
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.WAIVER_COLLECTION)
interface WaiverRepository : MongoRepository<Waiver, String> {
    @Query("{userId:'?0'}")
    fun findByUserId(userId: String): Waiver?

    fun findWaiverByStatus(status: WaiverStatus): List<Waiver>
}