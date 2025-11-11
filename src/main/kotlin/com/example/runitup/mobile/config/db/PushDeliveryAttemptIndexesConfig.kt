package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.PushDeliveryAttempt
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class PushDeliveryAttemptIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensurePushDeliveryAttemptIndexes() {
        val idx = mongoTemplate.indexOps(PushDeliveryAttempt::class.java)

        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("requestedAt", Sort.Direction.DESC)
                .named("user_requestedAt_desc_idx")
        )

        idx.createIndex(
            Index().on("sessionId", Sort.Direction.ASC)
                .on("requestedAt", Sort.Direction.DESC)
                .named("session_requestedAt_desc_idx")
        )

        idx.createIndex(
            Index().on("templateId", Sort.Direction.ASC)
                .on("requestedAt", Sort.Direction.DESC)
                .named("template_requestedAt_desc_idx")
        )

        idx.createIndex(Index().on("eventId", Sort.Direction.ASC).named("eventId_idx"))

        idx.createIndex(
            Index().on("tokenHash", Sort.Direction.ASC)
                .on("requestedAt", Sort.Direction.DESC)
                .named("tokenHash_requestedAt_desc_idx")
        )

        idx.createIndex(
            Index().on("vendor", Sort.Direction.ASC)
                .on("requestedAt", Sort.Direction.DESC)
                .named("vendor_requestedAt_desc_idx")
        )

        idx.createIndex(
            Index().on("vendor", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("requestedAt", Sort.Direction.DESC)
                .named("vendor_status_requestedAt_desc_idx")
        )

        // ✅ TTL index – automatically expire 90 days after expiresAt
        idx.createIndex(
            Index()
                .on("expiresAt", Sort.Direction.ASC)
                .named("ttl_expiresAt_idx")
                .expire(90L * 24 * 60 * 60) // 90 days
        )
    }
}
