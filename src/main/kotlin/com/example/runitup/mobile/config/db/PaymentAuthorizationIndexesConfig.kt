package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.PaymentAuthorization
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria

@Configuration
class PaymentAuthorizationIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensurePaymentAuthorizationIndexes() {
        val idx = mongoTemplate.indexOps(PaymentAuthorization::class.java)

        // Fast booking timelines (filter + sort)
        idx.createIndex(
            Index().on("bookingId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("booking_createdAt_desc_idx")
        )

        // Booking + role + time (PRIMARY/DELTA scans)
        idx.createIndex(
            Index().on("bookingId", Sort.Direction.ASC)
                .on("role", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("booking_role_createdAt_desc_idx")
        )

        // User + booking quick lookups
        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("bookingId", Sort.Direction.ASC)
                .named("user_booking_idx")
        )

        // Unique PaymentIntent
        idx.createIndex(
            Index().on("paymentIntentId", Sort.Direction.ASC)
                .unique()
                .named("paymentIntent_unique_idx")
        )

        // Customer reconciliation (status + time)
        idx.createIndex(
            Index().on("customerId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("customer_status_createdAt_desc_idx")
        )

        // Needs-user-action inbox
        idx.createIndex(
            Index().on("needsUserAction", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("needsUserAction_createdAt_desc_idx")
        )

        // Global recency
        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc_idx"))

        // ---- Partial indexes (MongoTemplate supports PartialIndexFilter) ----

        // Retry candidates: FAILED + TRANSIENT + CAPTURE
        val retryFilter = PartialIndexFilter.of(
            Criteria.where("status").`is`("FAILED")
                .and("failureKind").`is`("TRANSIENT")
                .and("lastOperation").`is`("CAPTURE")
        )
        idx.createIndex(
            Index().on("nextRetryAt", Sort.Direction.ASC)
                .on("retryCount", Sort.Direction.ASC)
                .partial(retryFilter)
                .named("retry_candidates_partial_idx")
        )

        // Enforce at most one PRIMARY per booking (unique + partial)
        val primaryFilter = PartialIndexFilter.of(Criteria.where("role").`is`("PRIMARY"))
        idx.createIndex(
            Index().on("bookingId", Sort.Direction.ASC)
                .on("role", Sort.Direction.ASC)
                .unique()
                .partial(primaryFilter)
                .named("one_primary_per_booking_idx")
        )
    }
}
