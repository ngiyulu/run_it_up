package com.example.runitup.mobile.utility

object CaptureFailurePolicy {
    data class Decision(
        val shouldRetry: Boolean,
        val backoffMs: Long = 0L
    )

    // attempt is 1-based (next attempt count)
    fun decide(attempt: Int): Decision {
        // allow at most 2 retries (attempt 1 -> schedule retry 1; attempt 2 -> schedule retry 2)
        // Backoff: 30s, then 2m
        return when (attempt) {
            1 -> Decision(shouldRetry = true, backoffMs = 30_000L)
            2 -> Decision(shouldRetry = true, backoffMs = 120_000L)
            else -> Decision(shouldRetry = false)
        }
    }

}