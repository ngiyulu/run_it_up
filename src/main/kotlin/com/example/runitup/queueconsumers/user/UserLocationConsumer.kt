package com.example.runitup.queueconsumers.user


import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.UserRepository
import com.example.runitup.mobile.rest.v1.controllers.runsession.CoordinateUpdateModel
import com.example.runitup.mobile.service.JobTrackerService
import com.example.runitup.mobile.service.LightSqsService
import com.example.runitup.queueconsumers.BaseQueueConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.stereotype.Component

@Component
class UserLocationConsumer(
    queueService: LightSqsService,
    appScope: CoroutineScope,
    private val trackerService: JobTrackerService,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository
): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.LOCATION_JOB, objectMapper) {

    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?, receiptHandle:String) {
        // Fetch up to 5 messages from the "jobs" queue
        logger.info("UserLocationConsumer is running")
        val data: JobEnvelope<CoordinateUpdateModel> = objectMapper.readValue(rawBody) as JobEnvelope<CoordinateUpdateModel>
        val payload = data.payload
        logger.info("payload = $payload")
        withContext(Dispatchers.IO) {
            val userDb = userRepository.findById(payload.userId)
            if(userDb.isPresent){
                val user = userDb.get()
                val coor = payload.coordinate
                user.coordinate = GeoJsonPoint(coor.longitude.toDouble(), coor.latitude.toDouble())
                userRepository.save(user)
            }

        }

    }


}
