package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.RefundRecord
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class RefundRecordIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureRefundIndexes() {
        val idx = mongoTemplate.indexOps(RefundRecord::class.java)

        // Idempotency
        idx.createIndex(Index().on("idempotencyKey", Sort.Direction.ASC)
            .unique()
            .named("idempotencyKey_unique_idx"))

        // Timelines / filters
        idx.createIndex(Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("user_createdAt_desc_idx"))
        idx.createIndex(Index().on("bookingId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("booking_createdAt_desc_idx"))
        idx.createIndex(Index().on("customerId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("customer_createdAt_desc_idx"))

        // Stripe linkage
        idx.createIndex(Index().on("paymentIntentId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("pi_createdAt_desc_idx"))
        idx.createIndex(Index().on("stripeRefundId", Sort.Direction.ASC)
            .named("stripeRefundId_idx"))

        // Status dashboards
        idx.createIndex(Index().on("status", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("status_createdAt_desc_idx"))


        idx.createIndex(Index().on("createdAt", Sort.Direction.ASC)
            .named("ttl_refunds_180d_idx")
            .expire(180L * 24 * 60 * 60)) // 180 days
    }
}
