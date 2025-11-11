package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.Waiver
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class WaiverIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureWaiverIndexes() {
        val idx = mongoTemplate.indexOps(Waiver::class.java)

        idx.createIndex(Index().on("userId", Sort.Direction.ASC).unique().sparse().named("userId_unique_idx"))
        idx.createIndex(Index().on("status", Sort.Direction.ASC).named("status_idx"))
        idx.createIndex(Index().on("status", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("status_createdAt_desc_idx"))
        idx.createIndex(Index().on("approvedBy", Sort.Direction.ASC).named("approvedBy_idx"))
        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc_idx"))
    }
}
