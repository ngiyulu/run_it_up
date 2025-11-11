package  com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.Booking
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class BookingIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureBookingIndexes() {
        val idx = mongoTemplate.indexOps(Booking::class.java)

        // Session queries: by status + timeline
        idx.createIndex(
            Index().on("runSessionId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("runSessionId_status_createdAt_desc_idx")
        )

        // User queries: by status + timeline
        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("userId_status_createdAt_desc_idx")
        )

        // Unique-ish matcher for quick GET (user + session + status)
        idx.createIndex(
            Index().on("userId", Sort.Direction.ASC)
                .on("runSessionId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("userId_runSessionId_status_idx")
        )

        // Generic timelines
        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc_idx"))
        idx.createIndex(Index().on("runSessionId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("runSessionId_createdAt_desc_idx"))
        idx.createIndex(Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("userId_createdAt_desc_idx"))

        // Payment drill-downs
        idx.createIndex(Index().on("customerId", Sort.Direction.ASC).named("customerId_idx"))
        idx.createIndex(Index().on("paymentMethodId", Sort.Direction.ASC).named("paymentMethodId_idx"))
        idx.createIndex(Index().on("paymentStatus", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("paymentStatus_createdAt_desc_idx"))

        // Waitlist / promotion analytics
        idx.createIndex(Index().on("status", Sort.Direction.ASC).on("promotedAt", Sort.Direction.ASC)
            .named("status_promotedAt_idx"))

        // Lock inspection
        idx.createIndex(Index().on("isLocked", Sort.Direction.ASC).on("isLockedAt", Sort.Direction.ASC)
            .named("lock_flag_time_idx"))

        // Optional: cleanup/cancellation dashboards
        idx.createIndex(Index().on("cancelledAt", Sort.Direction.ASC).named("cancelledAt_idx"))
        idx.createIndex(Index().on("completedAt", Sort.Direction.ASC).named("completedAt_idx"))

        // If you filter “active” rows often, consider:
        // idx.createIndex(Index().on("status", Sort.Direction.ASC).named("status_idx"))
    }
}

