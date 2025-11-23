package com.example.runitup.mobile.service

import com.example.runitup.mobile.repository.RunSessionEventRepository
import com.example.runitup.mobile.rest.v1.dto.Actor
import com.example.runitup.mobile.rest.v1.dto.RunSessionAction
import com.example.runitup.mobile.rest.v1.dto.RunSessionEvent
import org.springframework.stereotype.Service

@Service
class RunSessionEventLogger(
    private val repo: RunSessionEventRepository
) {
    fun log(
        sessionId: String,
        action: RunSessionAction,
        actor: Actor,
        subjectUserId: String? = null,
        reason: String? = null,
        metadata: Map<String, String?>? = null,
        prevStatus: String? = null,
        newStatus: String? = null,
        correlationId: String? = null,
        idempotencyKey: String? = null
    ) {
        val evt = RunSessionEvent(
            sessionId = sessionId,
            action = action,
            actor = actor,
            subjectUserId = subjectUserId,
            reason = reason,
            metadata = metadata,
            prevStatus = prevStatus,
            newStatus = newStatus,
            correlationId = correlationId,
            idempotencyKey = idempotencyKey
        )
        repo.save(evt)
    }
}