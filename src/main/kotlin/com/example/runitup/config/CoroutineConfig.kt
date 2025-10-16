package com.example.runitup.config

import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {
    /** A supervised scope for app background jobs (maintenance, consumers). */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Bean
    fun queueMaintenanceDispatcher(): CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(2) // tiny pool just for queue jobs

    @Bean
    fun appCoroutineScope(@Qualifier("queueMaintenanceDispatcher") disp: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + disp + CoroutineName("queue-bg"))
}