package com.example.runitup.mobile.service

import com.example.runitup.mobile.enum.PaymentStatus
import com.example.runitup.mobile.extensions.convertToCents
import com.example.runitup.mobile.model.AuthRole
import com.example.runitup.mobile.model.BookingStatus
import com.example.runitup.mobile.model.Payment
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.repository.BookingChangeEventRepository
import com.example.runitup.mobile.repository.BookingRepository
import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.rest.v1.dto.RunUser
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.param.SetupIntentCreateParams
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RunSessionService(): BaseService(){

    @Autowired
    lateinit var paymentService: PaymentService

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository


    @Autowired
    lateinit var bookingRepository: BookingRepository


    fun getRunSession(runSessionId:String): RunSession?{
        val db = runSessionRepository.findById(runSessionId)
        if(!db.isPresent){
            return  null
        }
        val bookings = bookingRepository.findByRunSessionIdAndStatusIn(runSessionId, mutableListOf(BookingStatus.WAITLISTED, BookingStatus.JOINED))
        val session = db.get()
        session.bookings = bookings.toMutableList()
        return  session
    }

    fun updateRunSession(runSession: RunSession): RunSession{
        runSession.bookings = mutableListOf()
        runSessionRepository.save(runSession)
        return runSession
    }


}