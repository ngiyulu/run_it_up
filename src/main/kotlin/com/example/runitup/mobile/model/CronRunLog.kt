package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

data class CronRunLog(
    @Id val id: String = ObjectId().toString(),
    val jobName: String,
    val startedAt: Instant,
    var finishedAt: Instant? = null,
    var status: CronStatus = CronStatus.RUNNING,
    var errorMessage: String? = null,
    var errorStack: String? = null,
    val nodeId: String,          // hostname or instance id
    val traceId: String? = null,
    var itemsProcessed: Int? = null,
    var meta: Map<String, String> = emptyMap()
)

enum class CronStatus { RUNNING, SUCCESS, FAILED }