package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.JobExecution
import jakarta.annotation.PostConstruct
import org.bson.Document
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition

@Configuration
class JobExecutionIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureJobExecutionIndexes() {
        val idx = mongoTemplate.indexOps(JobExecution::class.java)

        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("queue" to 1, "startedAt" to -1)))
                .named("queue_startedAt_desc_idx")
        )



        // Optional: index for status (for monitoring dashboards)
        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("status" to 1, "startedAt" to -1)))
                .named("status_startedAt_desc_idx")
        )
    }
}
