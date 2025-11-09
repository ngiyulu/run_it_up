package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.PushNotificationEvent
import com.example.runitup.mobile.model.RefundRecord
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PUSH_NOTIFICATION_COLLECTION)
interface PushNotificationEventRepository : MongoRepository<PushNotificationEvent, String> {
    fun findByDedupeKey(dedupeKey: String): PushNotificationEvent?
}