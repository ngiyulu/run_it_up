package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.Support
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class SupportIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureSupportIndexes() {
        val idx = mongoTemplate.indexOps(Support::class.java)

        idx.createIndex(
            Index().on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("status_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("email", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("email_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("admin.id", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("adminId_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("resolvedAt", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("resolvedAt_status_idx")
        )

        idx.createIndex(
            Index().on("createdAt", Sort.Direction.DESC)
                .named("createdAt_desc_idx")
        )

        // Optional: text search on name/description (only one text index per collection)
        // idx.createIndex(Index().text("name").text("description").named("text_name_description"))
    }
}
