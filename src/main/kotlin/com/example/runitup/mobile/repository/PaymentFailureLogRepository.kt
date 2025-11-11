package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.FailureKind
import com.example.runitup.mobile.model.PaymentFailureLog
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PAYMENT_FAILURE_COLLECTION)
interface PaymentFailureLogRepository : MongoRepository<PaymentFailureLog, String> {

    // Feeds / timelines
    fun findByBookingIdOrderByCreatedAtDesc(bookingId: String): List<PaymentFailureLog>
    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<PaymentFailureLog>
    fun findByPaymentIntentIdOrderByCreatedAtDesc(paymentIntentId: String): List<PaymentFailureLog>

    // Time-windowed views
    fun findByEventTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
        eventType: String,
        startInclusive: Long,
        endExclusive: Long
    ): List<PaymentFailureLog>

    fun findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        userId: String,
        startInclusive: Long,
        endExclusive: Long
    ): List<PaymentFailureLog>

    // Quick counts for dashboards
    fun countByBookingId(bookingId: String): Long
    fun countByPaymentIntentId(paymentIntentId: String): Long
    fun countByFailureKind(failureKind: FailureKind): Long
}
