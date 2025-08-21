// src/main/kotlin/com/example/config/SchedulingConfig.kt
package com.example.runitup.crash

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

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
