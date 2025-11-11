package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.WaitlistSetupState
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class WaitlistSetupStateIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureIndexes() {
        val idx = mongoTemplate.indexOps(WaitlistSetupState::class.java)

        idx.createIndex(Index().on("setupIntentId", Sort.Direction.ASC).unique().sparse().named("setupIntentId_unique_idx"))
        idx.createIndex(Index().on("bookingId", Sort.Direction.ASC).on("paymentMethodId", Sort.Direction.ASC).named("booking_payment_idx"))
        idx.createIndex(Index().on("sessionId", Sort.Direction.ASC).named("sessionId_idx"))
        idx.createIndex(Index().on("userId", Sort.Direction.ASC).on("status", Sort.Direction.ASC).named("user_status_idx"))
        idx.createIndex(Index().on("customerId", Sort.Direction.ASC).on("paymentMethodId", Sort.Direction.ASC).named("customer_payment_idx"))
        idx.createIndex(Index().on("needsUserAction", Sort.Direction.ASC).named("needsUserAction_idx"))
        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc_idx"))

        // Optional TTL (e.g. 30 days)
        // idx.createIndex(Index().on("createdAt", Sort.Direction.ASC).expire(2592000L).named("ttl_createdAt_30d_idx"))
    }
}
