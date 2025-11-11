//package com.example.runitup.queueconsumers.user
//
//
//import com.example.runitup.mobile.model.JobEnvelope
//import com.example.runitup.mobile.queue.QueueNames
//import com.example.runitup.mobile.repository.UserRepository
//import com.example.runitup.mobile.service.JobTrackerService
//import com.example.runitup.mobile.service.LightSqsService
//import com.example.runitup.queueconsumers.BaseQueueConsumer
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.springframework.stereotype.Component
//
//@Component
//class NewUserConsumer(
//    queueService: LightSqsService,
//    appScope: CoroutineScope,
//    private val trackerService: JobTrackerService,
//    private val objectMapper: ObjectMapper,
//    private val userRepository: UserRepository
//): BaseQueueConsumer(queueService, appScope, trackerService, QueueNames.NEW_USER_JOB, objectMapper) {
//
//
//    override suspend fun processOne(rawBody: String, taskType: String, jobId: String, traceId: String?) {
//        // Fetch up to 5 messages from the "jobs" queue
//        logger.info("UserLocationConsumer is running")
//        val data: JobEnvelope<String> = objectMapper.readValue(rawBody) as JobEnvelope<String>
//        val payload = data.payload
//        withContext(Dispatchers.IO) {
//            val userDb = userRepository.findById(payload)
//            val user = userDb.get()
//            //TODO: send text message to say welcome
//        }
//
//    }
//
//
//}
