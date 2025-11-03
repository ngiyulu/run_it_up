package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.JobExecution
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.JOB_EXECUTION_COLLECTION)
interface JobExecutionRepository : MongoRepository<JobExecution, String> {
    fun findByJobId(jobId: String): List<JobExecution>
}