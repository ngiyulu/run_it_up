//package com.example.runitup.queueconsumers
//
//import com.example.runitup.mobile.model.JobEnvelope
//import com.example.runitup.mobile.queue.QueueNames
//import com.example.runitup.mobile.service.LightSqsService
//import kotlinx.coroutines.runBlocking
//import org.slf4j.LoggerFactory
//import org.springframework.boot.CommandLineRunner
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import java.time.Instant
//import java.util.*
//
//@Configuration
//class QueueTestRunner {
//
//    private val log = LoggerFactory.getLogger(javaClass)
//
//    @Bean
//    fun testQueue(lightSqsService: LightSqsService): CommandLineRunner = CommandLineRunner {
//        runBlocking {
//            val queue = QueueNames.JOB_WAIT_LIST
//
//            // Ensure the queue exists
//            lightSqsService.createQueue(queue)
//
//            // Enqueue 3 demo jobs
//            repeat(3) { i ->
//                val env = JobEnvelope(
//                    jobId = UUID.randomUUID().toString(),
//                    taskType = "DEMO_TASK",
//                    payload = "68fbf049eac0bdb7c37e1b6a",
//                    traceId = UUID.randomUUID().toString(),
//                    createdAtMs = Instant.now()
//                )
//                val id = lightSqsService.sendJob(queue, env)
//                log.info("Enqueued job $i -> $id")
//            }
//
//            log.info("✅ Test jobs enqueued successfully into '$queue'")
//        }
//    }
//}
