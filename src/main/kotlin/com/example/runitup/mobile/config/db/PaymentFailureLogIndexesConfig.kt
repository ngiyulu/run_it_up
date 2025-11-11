package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.PaymentFailureLog
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class PaymentFailureLogIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensurePaymentFailureLogIndexes() {
        val idx = mongoTemplate.indexOps(PaymentFailureLog::class.java)

        idx.createIndex(
            Index().on("bookingId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("booking_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("user_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("paymentIntentId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("pi_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("eventType", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("event_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("failureKind", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("failureKind_createdAt_desc_idx")
        )

        idx.createIndex(
            Index().on("bookingId", Sort.Direction.ASC)
                .on("attempt", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("booking_attempt_createdAt_idx")
        )

        // ✅ TTL index — automatically expire after 90 days
        // (Mongo will remove documents where `createdAt` + expireAfterSeconds < now)
        idx.createIndex(
            Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(90L * 24 * 60 * 60) // 90 days
                .named("ttl_createdAt_90d_idx")
        )
    }
}
