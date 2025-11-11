package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.Phone
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class PhoneIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensurePhoneIndexes() {
        val idx = mongoTemplate.indexOps(Phone::class.java)

        // Unique token: ensures one record per push token
        idx.createIndex(
            Index().on("token", Sort.Direction.ASC)
                .unique()
                .named("token_unique_idx")
        )

        // Lookup by phoneId (logical device ID)
        idx.createIndex(
            Index().on("phoneId", Sort.Direction.ASC)
                .named("phoneId_idx")
        )

        // Lookup by userId
        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .named("userId_idx")
        )

        // Combined user + type for quick platform filter
        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("type", Sort.Direction.ASC)
                .named("userId_type_idx")
        )

        // Platform-level filtering
        idx.createIndex(
            Index().on("type", Sort.Direction.ASC)
                .named("type_idx")
        )

        // (Optional) Sort by creation time for recent device lists
        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("userId_createdAt_desc_idx")
        )
    }
}
