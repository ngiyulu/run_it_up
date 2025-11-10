package com.example.runitup.mobile.rest.v1.dto

import org.springframework.data.annotation.Id
import java.time.Instant

data class RunSessionEvent(
    @Id val id: String? = null,
    val sessionId: String,
    val ts: Instant = Instant.now(),
    val action: RunSessionAction,
    val actor: Actor,
    val subjectUserId: String? = null,
    val reason: String? = null,
    val metadata: Map<String, Any?>? = null,
    val prevStatus: String? = null,
    val newStatus: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

data class Actor(
    val type: ActorType,     // USER / ADMIN / SYSTEM
    val id: String? = null   // null for SYSTEM if no concrete id
)

enum class ActorType { USER, ADMIN, SYSTEM }