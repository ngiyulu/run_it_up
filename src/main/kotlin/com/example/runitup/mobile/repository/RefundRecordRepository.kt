package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.RefundRecord
import com.example.runitup.mobile.model.RefundStatus
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Document(collection = CollectionConstants.REFUND_COLLECTION)
interface RefundRecordRepository : MongoRepository<RefundRecord, String> {
    // Existing
    fun findByPaymentIntentId(paymentIntentId: String): List<RefundRecord>
    fun findByBookingId(bookingId: String): List<RefundRecord>

    // 🔎 Common lookups
    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<RefundRecord>
    fun findByCustomerIdOrderByCreatedAtDesc(customerId: String): List<RefundRecord>
    fun findByIdempotencyKey(idempotencyKey: String): RefundRecord?

    // 🕒 Time windows (for dashboards/reports)
    fun findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
        status: RefundStatus,
        start: Instant,
        end: Instant
    ): List<RefundRecord>

    fun findByPaymentIntentIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        paymentIntentId: String,
        start: Instant,
        end: Instant
    ): List<RefundRecord>

    // 📊 Quick counts
    fun countByStatus(status: RefundStatus): Long
    fun countByBookingId(bookingId: String): Long
}