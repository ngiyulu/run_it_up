// src/main/kotlin/com/example/config/SchedulingConfig.kt
package com.example.runitup.mobile.crash

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler


/*
Spring runs those scheduled tasks on a single-threaded default scheduler (internally a ThreadPoolTaskScheduler with one thread).
All cron jobs share one thread.

If one job runs long (say 2 minutes), the next scheduled job will wait — it won’t run until the previous one finishes.

It also competes with your app’s other workloads if you’re not careful.

So SchedulerConfig replaces that single-threaded scheduler with a custom thread pool dedicated to cron jobs — completely separate from your web or async executor threads.
@EnableScheduling

Tells Spring Boot:

“Scan for any @Scheduled annotations in this project and enable them.”

Without this annotation, your @Scheduled jobs will never run.
 */
@Configuration
@EnableScheduling
class SchedulingConfig {
    private val log = LoggerFactory.getLogger(SchedulingConfig::class.java)

    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler {
        val ts = ThreadPoolTaskScheduler()
        ts.poolSize = 4
        ts.setThreadNamePrefix("sched-")
        ts.setErrorHandler { ex -> log.error("Uncaught @Scheduled error", ex) }
        ts.setTaskDecorator(MdcTaskDecorator())
        ts.initialize()
        return ts
    }
}
