package com.example.runitup

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class RunItUpApplication{
    private val log = LoggerFactory.getLogger(RunItUpApplication::class.java)
    @Bean
    fun defaultUncaughtHandler(): ApplicationRunner = ApplicationRunner {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.error("Process-level uncaught exception in thread ${t.name}", e)
            // Optional: trigger graceful shutdown here if needed
        }
    }
}

fun main(args: Array<String>) {
    runApplication<RunItUpApplication>(*args)
}
