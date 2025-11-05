package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.Otp
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingPaymentStateRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.mongodb.client.result.UpdateResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RunSessionService(): BaseService(){

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository


    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var bookingPaymentStateRepository: BookingPaymentStateRepository


    fun getRunSession(runSessionId:String, userId:String? = null): RunSession?{
        val db = runSessionRepository.findById(runSessionId)
        if(!db.isPresent){
            return  null
        }
        val bookings = bookingRepository.findByRunSessionIdAndStatusIn(runSessionId, mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED))
        val session = db.get()
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

        return mongoTemplate.updateFirst(q, u, RunSession::class.java)
    }

    fun updateRunSession(runSession: RunSession): RunSession{
        runSession.bookings = mutableListOf()
        runSessionRepository.save(runSession)
        return runSession
    }


}