package com.example.runitup.mobile.service

import com.example.runitup.mobile.cache.MyCacheManager
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.JobEnvelope
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.queue.QueueNames
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.service.BookingDbService
import com.example.runitup.mobile.rest.v1.dto.PushJobModel
import com.example.runitup.mobile.rest.v1.dto.PushJobType
import com.example.runitup.mobile.rest.v1.dto.session.StartSessionModel
import com.example.runitup.mobile.service.payment.BookingPricingAdjuster
import com.example.runitup.mobile.service.push.RunSessionPushNotificationService
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RunSessionService(): BaseService(){

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var cacheManager: MyCacheManager

    @Autowired
    lateinit var bookingDbService: BookingDbService

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var queueService: LightSqsService

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository

    @Autowired
    lateinit var bookingPricingAdjuster: BookingPricingAdjuster

    @Autowired
    lateinit var appScope: CoroutineScope

    @Autowired
    lateinit var numberGenerator: NumberGenerator


    fun getBooking(runSessionId: String):List<Booking>{
        return bookingRepository.findByRunSessionIdAndStatusIn(runSessionId, mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED))
    }
    fun getRunSession(runSessionId:String, userId:String? = null): RunSession?{
        val session  = cacheManager.getRunSession(runSessionId) ?: return  null
        session.code?.let {
            session.plain= numberGenerator.decryptEncryptedCode(it)
        }
        val bookings = getBooking(runSessionId)
        session.bookings = bookings.toMutableList()
        if(userId != null){
            session.getBooking(userId)?.let {
                // we want to send the user the bookingPaymentState that way
                // if they have a booking, they can see what payment they used
                session.bookingPaymentState = bookingPaymentStateRepository.findByBookingId(it.bookingId)
            }
        }
        return  session
    }

    fun confirmRunSession(runSessionId:String): UpdateResult{
        val q = Query(
            Criteria.where("_id").`is`(runSessionId)
                .and("status").`is`(RunStatus.CONFIRMED)
        )
        val u = Update()
            .set("status", RunStatus.CONFIRMED)
            .set("confirmedAt", Instant.now())

        cacheManager.evictRunSession(runSessionId)
        return mongoTemplate.updateFirst(q, u, RunSession::class.java)
    }

    fun updateRunSession(runSession: RunSession): RunSession{
        runSession.bookings = mutableListOf()
        cacheManager.updateRunSession(runSession)
        return runSession
    }

    fun startConfirmationProcess(run: RunSession, actor:String): RunSession{
        val bookingList  = bookingDbService.getBookingList(run.id.orEmpty())
        run.bookings = bookingList.toMutableList()
        if(!run.isSessionFree()){
            // we captured the charge in stripe
            // we updated the booking list payment status
            // we created the payment list that needs stored in the payment db
            bookingList.forEach {
                bookingPricingAdjuster.createPrimaryHoldWithChange(
                    it.id.orEmpty(),
                    it.userId,
                    it.customerId.orEmpty(),
                    "usd",
                    it.currentTotalCents,
                    it.paymentMethodId.orEmpty(),
                    actor
                    )
            }
        }

        appScope.launch {
            val data = JobEnvelope(
                jobId = UUID.randomUUID().toString(),
                taskType = "Process payment after confirmation from job",
                payload = run.id.orEmpty()
            )
            queueService.sendJob(QueueNames.RUN_PROCESS_PAYMENT, data)
        }
        run.status = RunStatus.CONFIRMED
        val data = JobEnvelope(
            jobId = UUID.randomUUID().toString(),
            taskType = "Notification users of a run session confirmation",
            payload = PushJobModel(PushJobType.CONFIRM_RUN, run.id.orEmpty())
        )
        appScope.launch {
            queueService.sendJob(QueueNames.RUN_SESSION_PUSH_JOB, data)
        }
        return updateRunSession(run)
    }

    fun startRunSession(model: StartSessionModel, run: RunSession, adminId: String? = null): StartRunSessionModel{
        if(run.status != RunStatus.CONFIRMED){
            return StartRunSessionModel(StartRunSessionModelEnum.CONFIRMED)
        }
        if(run.status == RunStatus.ONGOING){
            return  StartRunSessionModel(StartRunSessionModelEnum.SUCCESS, run)
        }
        run.status = RunStatus.ONGOING
        run.startedAt = Instant.now()
        adminId?.let {
            run.startedBy = it
        }

        val updated = updateRunSession(run)
        return StartRunSessionModel(StartRunSessionModelEnum.SUCCESS, updated)
    }

    

}

class StartRunSessionModel(val status:StartRunSessionModelEnum, val session: RunSession? = null)
enum class StartRunSessionModelEnum{
    INVALID_ID, CONFIRMED, SUCCESS
}