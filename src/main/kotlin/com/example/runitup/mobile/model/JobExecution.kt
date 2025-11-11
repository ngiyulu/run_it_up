package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

enum class JobStatus { STARTED, SUCCEEDED, FAILED }


data class JobExecution(
    @Id var id: String? = ObjectId().toHexString(),

    val jobId: String,
    val queue: String,
    val taskType: String,
    val workerId: String,           // hostname/pod + consumer class name
    val attempt: Int,               // attempt # from envelope or derived from receiveCount
    val receiveCount: Int,          // from your Redis wrapper
    val traceId: String? = null,

    val startedAt: Instant = Instant.now(),
    var finishedAt: Instant? = null,
    var status: JobStatus = JobStatus.STARTED,
    var errorType: String? = null,
    var errorMessage: String? = null,
    var errorStack: String? = null
)