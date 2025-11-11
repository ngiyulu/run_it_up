package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.rest.v1.dto.RunSessionEvent
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class RunSessionEventIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureIndexes() {
        val idx = mongoTemplate.indexOps(RunSessionEvent::class.java)

        idx.createIndex(Index().on("sessionId", Sort.Direction.ASC).on("ts", Sort.Direction.ASC)
            .named("session_ts_asc_idx"))

        idx.createIndex(Index().on("sessionId", Sort.Direction.ASC).on("action", Sort.Direction.ASC)
            .on("ts", Sort.Direction.ASC).named("session_action_ts_idx"))

        idx.createIndex(Index().on("actor.id", Sort.Direction.ASC).on("ts", Sort.Direction.DESC)
            .named("actorId_ts_desc_idx"))
        idx.createIndex(Index().on("actor.type", Sort.Direction.ASC).on("ts", Sort.Direction.DESC)
            .named("actorType_ts_desc_idx"))

        idx.createIndex(Index().on("sessionId", Sort.Direction.ASC).on("subjectUserId", Sort.Direction.ASC)
            .on("ts", Sort.Direction.ASC).named("session_subject_ts_idx"))

        idx.createIndex(Index().on("correlationId", Sort.Direction.ASC)
            .named("correlationId_idx"))
        idx.createIndex(Index().on("idempotencyKey", Sort.Direction.ASC)
            .unique().sparse().named("idempotencyKey_unique_idx"))

        idx.createIndex(Index().on("sessionId", Sort.Direction.ASC)
            .on("prevStatus", Sort.Direction.ASC).on("newStatus", Sort.Direction.ASC)
            .on("ts", Sort.Direction.ASC).named("session_transition_ts_idx"))

        idx.createIndex(Index().on("ts", Sort.Direction.DESC).named("ts_desc_idx"))
    }
}
