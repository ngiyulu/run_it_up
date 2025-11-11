package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.BookingPaymentState
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class BookingPaymentStateIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureBookingPaymentStateIndexes() {
        val idx = mongoTemplate.indexOps(BookingPaymentState::class.java)

        idx.createIndex(Index().on("bookingId", Sort.Direction.ASC).named("bookingId_idx"))
        idx.createIndex(Index().on("userId", Sort.Direction.ASC).named("userId_idx"))
        idx.createIndex(Index().on("customerId", Sort.Direction.ASC).named("customerId_idx"))
        idx.createIndex(Index().on("status", Sort.Direction.ASC)
            .on("latestUpdatedAt", Sort.Direction.DESC)
            .named("status_updatedAt_idx"))
        idx.createIndex(Index().on("latestUpdatedAt", Sort.Direction.DESC).named("updatedAt_desc_idx"))
        idx.createIndex(Index().on("userId", Sort.Direction.ASC)
            .on("status", Sort.Direction.ASC)
            .on("latestUpdatedAt", Sort.Direction.DESC)
            .named("user_status_updatedAt_idx"))
    }
}
