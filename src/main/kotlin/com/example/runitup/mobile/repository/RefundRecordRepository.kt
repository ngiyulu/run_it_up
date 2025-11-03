package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.RefundRecord
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.REFUND_COLLECTION)
interface RefundRecordRepository : MongoRepository<RefundRecord, String> {
    fun findByPaymentIntentId(paymentIntentId: String): List<RefundRecord>
    fun findByBookingId(bookingId: String): List<RefundRecord>
}