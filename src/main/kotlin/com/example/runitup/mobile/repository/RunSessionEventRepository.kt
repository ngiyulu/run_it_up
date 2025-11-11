package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.rest.v1.dto.ActorType
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.RunSessionEvent
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Document(collection = CollectionConstants.RUN_SESSION_EVENTS_COLLECTION)
interface RunSessionEventRepository : MongoRepository<RunSessionEvent, String> {

    // Existing timeline (asc)
    fun findBySessionIdOrderByTsAsc(sessionId: String): List<RunSessionEvent>

    // Most-used variants
    fun findBySessionIdOrderByTsDesc(sessionId: String): List<RunSessionEvent>
    fun findBySessionIdAndTsBetweenOrderByTsAsc(
        sessionId: String, start: Instant, end: Instant
    ): List<RunSessionEvent>

    // Filter by action or actor
    fun findBySessionIdAndActionOrderByTsAsc(sessionId: String, action: RunSessionAction): List<RunSessionEvent>
    fun findByActor_IdOrderByTsDesc(actorId: String): List<RunSessionEvent>
    fun findByActor_TypeOrderByTsDesc(actorType: ActorType): List<RunSessionEvent>

    // Subject drill-down (e.g., user joins/left/kicked)
    fun findBySessionIdAndSubjectUserIdOrderByTsAsc(sessionId: String, subjectUserId: String): List<RunSessionEvent>

    // Correlation / idempotency
    fun findByCorrelationId(correlationId: String): List<RunSessionEvent>
    fun findByIdempotencyKey(idempotencyKey: String): RunSessionEvent?

    // Status transitions
    fun findBySessionIdAndPrevStatusAndNewStatusOrderByTsAsc(
        sessionId: String, prevStatus: String, newStatus: String
    ): List<RunSessionEvent>

    // Time-window ops (dashboards)
    fun findByTsBetweenOrderByTsDesc(start: Instant, end: Instant): List<RunSessionEvent>
}

