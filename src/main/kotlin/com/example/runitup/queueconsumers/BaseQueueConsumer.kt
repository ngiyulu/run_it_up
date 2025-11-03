package com.example.runitup.queueconsumers

import com.example.runitup.mobile.service.LightSqsService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

abstract class BaseQueueConsumer(
    private val queueService: LightSqsService,
    private val appScope: CoroutineScope) {

    protected val log = LoggerFactory.getLogger(javaClass)
    @PostConstruct
    fun startPolling(){
        // Launch a coroutine to poll every 30 seconds
        appScope.launch(CoroutineName(coroutineName())) {
            while (isActive) {
                try {
                    processData()
                } catch (e: Exception) {
                    log.error("Error during queue poll", e)
                }
                delay(30_000) // 30 seconds between polls
            }
        }
    }

    abstract suspend fun processData()

    private fun coroutineName () = this::class::simpleName.name
}