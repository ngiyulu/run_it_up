package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.AppReview
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class AppReviewIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureAppReviewIndexes() {
        val idx = mongoTemplate.indexOps(AppReview::class.java)

        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc_idx"))
        idx.createIndex(Index().on("star", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("star_createdAt_desc_idx"))
        idx.createIndex(Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("user_createdAt_desc_idx"))

        // Optional:
        // idx.createIndex(Index().on("userId", Sort.Direction.ASC).unique().named("user_unique_idx"))
        // idx.createIndex(Index().text("feedback").named("text_feedback_idx"))
    }
}
