package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.service.ActionStatus
import com.example.runitup.mobile.service.UserActionRequired
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.USER_ACTION_COLLECTION)
interface UserActionRequiredRepository : MongoRepository<UserActionRequired, String> {
    fun findByUserIdAndStatusInOrderByPriorityAscCreatedAtAsc(
        userId: String,
        statuses: List<ActionStatus>
    ): List<UserActionRequired>

    fun findByUserIdAndDedupeKey(userId: String, dedupeKey: String): UserActionRequired?
}