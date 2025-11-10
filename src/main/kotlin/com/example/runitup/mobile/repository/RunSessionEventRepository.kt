package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.rest.v1.dto.RunSessionEvent
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.RUN_SESSION_EVENTS_COLLECTION)
interface RunSessionEventRepository : MongoRepository<RunSessionEvent, String> {
    fun findBySessionIdOrderByTsAsc(sessionId: String): List<RunSessionEvent>
}
