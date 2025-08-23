package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.Payment
import com.twilio.twiml.voice.Pay
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PAYMENT_COLLECTION)
interface PaymentRepository: MongoRepository<Payment, String> {

    @Query("{bookingId:'?0'}")
    fun findByBookingId(bookingId: String): List<Payment>

    @Query("{sessionId:'?0'}")
    fun findByRunSessionId(bookingId: String): List<Payment>
}