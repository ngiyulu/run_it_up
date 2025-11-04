package com.example.runitup.mobile.model

import java.time.Instant

data class JobEnvelope<T>(
    val jobId: String = "",              // stable id (producer sets)
    val taskType: String = "",           // e.g., "PUSH_NOTIFY", "CAPTURE_PI", "PROMOTE_WAITLIST"
    val payload: T,                 // your business payload
    val attempt: Int = 1,           // consumer can bump; we also have receiveCount in Redis
    val traceId: String? = null,    // for logs/tracing
    val createdAtMs: Instant = Instant.now()
)