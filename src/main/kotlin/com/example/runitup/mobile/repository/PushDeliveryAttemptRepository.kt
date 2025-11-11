package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.AttemptStatus
import com.example.runitup.mobile.model.PushDeliveryAttempt
import com.example.runitup.mobile.model.PushVendor
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Document(collection = CollectionConstants.PUSH_DELIVERY_ATTEMPT_COLLECTION)
interface PushDeliveryAttemptRepository : MongoRepository<PushDeliveryAttempt, String> {

    // Feeds / timelines
    fun findByUserIdOrderByRequestedAtDesc(userId: String): List<PushDeliveryAttempt>
    fun findBySessionIdOrderByRequestedAtDesc(sessionId: String): List<PushDeliveryAttempt>
    fun findByTemplateIdOrderByRequestedAtDesc(templateId: String): List<PushDeliveryAttempt>

    // Quick lookup
    fun findByEventId(eventId: String): List<PushDeliveryAttempt>          // often 1..N if retried
    fun findByTokenHash(tokenHash: String): List<PushDeliveryAttempt>

    // Time-windowed dashboards
    fun findByVendorAndRequestedAtBetweenOrderByRequestedAtDesc(
        vendor: PushVendor,
        start: Instant,
        end: Instant
    ): List<PushDeliveryAttempt>

    fun findByTemplateIdAndRequestedAtBetweenOrderByRequestedAtDesc(
        templateId: String,
        start: Instant,
        end: Instant
    ): List<PushDeliveryAttempt>

    // Health / ops
    fun countByStatus(status: AttemptStatus): Long
    fun countByVendorAndStatus(vendor: PushVendor, status: AttemptStatus): Long
}
