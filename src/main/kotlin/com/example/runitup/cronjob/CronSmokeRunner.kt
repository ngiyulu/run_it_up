//package com.example.runitup.cronjob
//
//import org.springframework.boot.CommandLineRunner
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//
//@Configuration
//class CronSmokeRunner {
//    @Bean
//    fun cronSmoke(runner: CronJobRunner) = CommandLineRunner {
//        runner.runCron("smoke-demo", ttlSeconds = 30) { audit ->
//            audit.putMeta("hello", "world")
//            audit.incrementProcessedBy(3)
//        }
//    }
//}
