package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.JobExecution
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Document(collection = CollectionConstants.JOB_EXECUTION_COLLECTION)
interface JobExecutionRepository : MongoRepository<JobExecution, String> {
    fun findByQueue(queue:String): List<JobExecution>

    // 🔍 Filter by queue and start time range
    fun findByQueueAndStartedAtBetween(
        queue: String,
        startInclusive: Instant,
        endExclusive: Instant
    ): List<JobExecution>

    // (Optional) For dashboards, sort by newest first
    fun findByQueueOrderByStartedAtDesc(queue: String): List<JobExecution>
}