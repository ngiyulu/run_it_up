package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.PhoneRepository
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class BadTokenConsumer(
    queueService: LightSqsService,
    appScope: CoroutineScope,
    trackerService: JobTrackerService,
    private val phoneRepository: PhoneRepository,
    private val objectMapper: ObjectMapper,
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.BAD_TOKEN_JOB, objectMapper) {

    override fun delay(): Long = 2_000          // poll every 2s
    override fun waitSeconds(): Int = 5         // short poll but responsive
    override fun maxNumberOfMessages(): Int = 10
    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?, receiptHandle:String) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("BadTokenConsumer is running")
        val envelope: JobEnvelope<List<String>> = objectMapper.readValue(rawBody) as JobEnvelope<List<String>>
        withContext(Dispatchers.IO) {
            val tokens = envelope.payload
            if (tokens.isEmpty()) {
                logger.warn("No tokens provided for deletion")
                return@withContext
            }
            val deletedCount = phoneRepository.deleteAllByTokenIn(tokens)
            logger.info("Deleted $deletedCount invalid push tokens")
        }
    }
}
