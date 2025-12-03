package com.example.runitup.queueconsumers.run


import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.mobile.service.NearbyUserService
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class NewRunSessionConsumer(
    queueService: LightSqsService,
    appScope: CoroutineScope,
    trackerService: JobTrackerService,
    private val nearbyUserService: NearbyUserService,
    private val cacheManager: MyCacheManager,
    private val runSessionPushNotificationService: RunSessionPushNotificationService,
    private val objectMapper: ObjectMapper,
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.NEW_RUN_JOB, objectMapper) {

    override fun delay(): Long = 2_000          // poll every 2s
    override fun waitSeconds(): Int = 5         // short poll but responsive
    override fun maxNumberOfMessages(): Int = 10
    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?, receiptHandle:String) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("NewRunSessionConsumer is running")
        val env: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
        logger.info(env.toString())
        withContext(Dispatchers.IO) {
            val run = cacheManager.getRunSession(env.payload)
            if(run == null){
                logger.error("couldn't find run id ${env.payload} from NewRunSessionConsumer")
                return@withContext
            }
            val users = nearbyUserService.findUsersNearRunSession(run, 20.0)
            logger.info("${users.size} users will get notified for the new session")
            users.forEach {
                logger.info("notifying ${it.getFullName()}")
                runSessionPushNotificationService.notifyUserNewRunCreated(
                    it.id.orEmpty(),
                    run
                )
            }
        }
    }
}
