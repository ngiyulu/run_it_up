package com.example.runitup.cronjob.jobs

// jobs/ExpiredCaptureRetryJob.kt

import com.example.runitup.cronjob.CronJobRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExpiredCaptureRetryJob(
    private val runner: CronJobRunner,
    // inject your repositories/services as needed
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Every minute (adjust as needed). Uses the dedicated scheduler pool.
    @Scheduled(cron = "0 * * * * *")
    fun schedule() {
        runner.runCron(jobName = "expired-capture-retry", ttlSeconds = 300) { audit ->
            // example job body — NEVER blocks HTTP threads
            withContext(Dispatchers.IO) {
                // 1) query pending capture retries
                // val items = paymentAuthorizationRepo.findRetryCandidates(Instant.now(), limit = 200)

                // fake loop
                repeat(5) { idx ->
                    // do work…
                    // on each success:
                    audit.incrementProcessedBy(1)
                }
                // Add any custom metadata
                audit.putMeta("note", "processed demo batch")

            }
        }
    }
}
