package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.PushNotificationEvent
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class PushNotificationEventIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureIndexes() {
        val idx = mongoTemplate.indexOps(PushNotificationEvent::class.java)

        idx.createIndex(Index().on("dedupeKey", Sort.Direction.ASC)
            .unique().sparse().named("dedupeKey_unique_idx"))

        idx.createIndex(Index().on("trigger", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC)
            .named("trigger_createdAt_desc_idx"))

        idx.createIndex(Index().on("templateId", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC)
            .named("template_createdAt_desc_idx"))

        idx.createIndex(Index().on("triggerRefId", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC)
            .named("triggerRef_createdAt_desc_idx"))

        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC)
            .named("createdAt_desc_idx"))

        // TTL — choose ONE:
        // A) Per-doc TTL using expiresAt:
        idx.createIndex(Index().on("expiresAt", Sort.Direction.ASC)
            .named("ttl_expiresAt_idx")
            .expire(0))

        // B) Fixed 90-day TTL from createdAt:
        // idx.createIndex(Index().on("createdAt", Sort.Direction.ASC)
        //     .named("ttl_createdAt_90d_idx")
        //     .expire(90L * 24 * 60 * 60))
    }
}
