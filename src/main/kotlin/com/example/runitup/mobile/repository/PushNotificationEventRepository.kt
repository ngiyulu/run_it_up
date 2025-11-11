package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.PushNotificationEvent
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Document(collection = CollectionConstants.PUSH_NOTIFICATION_COLLECTION)
interface PushNotificationEventRepository : MongoRepository<PushNotificationEvent, String> {

    fun findByDedupeKey(dedupeKey: String): PushNotificationEvent?

    // Timelines / reporting
    fun findAllByTriggerOrderByCreatedAtDesc(trigger: String): List<PushNotificationEvent>
    fun findAllByTemplateIdOrderByCreatedAtDesc(templateId: String): List<PushNotificationEvent>
    fun findAllByCreatedAtBetweenOrderByCreatedAtDesc(start: Instant, end: Instant): List<PushNotificationEvent>

    // Scoped to a specific “thing” (e.g., a runSession)
    fun findAllByTriggerRefIdOrderByCreatedAtDesc(triggerRefId: String): List<PushNotificationEvent>

    // Counts for dashboards
    fun countByTrigger(trigger: String): Long
    fun countByTemplateId(templateId: String): Long
}
