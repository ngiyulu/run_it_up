package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.RunSession
import jakarta.annotation.PostConstruct
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition

@Configuration
class MongoIndexConfig(private val mongoTemplate: MongoTemplate) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun ensureIndexes() {
        val indexOps = mongoTemplate.indexOps(RunSession::class.java)

        // Define your compound index
        val indexDoc = Document(
            mapOf(
                "location" to "2dsphere",
                "privateRun" to 1,
                "status" to 1,
                "date" to 1,
                "startTime" to 1
            )
        )

        val indexDefinition = CompoundIndexDefinition(indexDoc).named("loc_priv_status_date_time_idx")
        // --- New date index ---
        val dateIndexDoc = Document(mapOf("date" to 1))
        indexOps.createIndex(CompoundIndexDefinition(dateIndexDoc).named("date_idx"))

        // --- New status index ---
        val statusIndexDoc = Document(mapOf("status" to 1))
        indexOps.createIndex(CompoundIndexDefinition(statusIndexDoc).named("status_idx"))

        // ✅ Use createIndex() instead of ensureIndex()
        indexOps.createIndex(indexDefinition)

        log.info("✅ Created MongoDB index: loc_priv_status_date_time_idx (if not existing)")
    }
}
