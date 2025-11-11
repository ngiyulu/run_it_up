package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.BookingChangeEvent
import jakarta.annotation.PostConstruct
import org.bson.Document
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition

@Configuration
class BookingChangeEventIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureBookingChangeEventIndexes() {
        val idx = mongoTemplate.indexOps(BookingChangeEvent::class.java)

        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("userId" to 1, "createdAt" to -1)))
                .named("user_createdAt_desc_idx")
        )

        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("bookingId" to 1, "version" to 1)))
                .named("booking_version_unique_idx")
        )

        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("bookingId" to 1, "createdAt" to -1)))
                .named("booking_createdAt_desc_idx")
        )
    }
}
