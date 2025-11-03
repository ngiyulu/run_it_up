package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.PaymentAuthorization
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository


object PaymentAuthorizationQueries {
    const val CAPTURE_RETRY_CANDIDATES =
        "{ 'status': 'FAILED', 'failureKind': 'TRANSIENT', 'lastOperation': 'CAPTURE', " +
                "'nextRetryAt': { '\$lte': ?0 }, 'retryCount': { '\$lt': 2 } }"
}
@Repository
@Document(collection = CollectionConstants.PAYMENT_AUTHORIZATION)
interface PaymentAuthorizationRepository : MongoRepository<PaymentAuthorization, String> {


    fun findByBookingId(bookingId: String): List<PaymentAuthorization>

    fun findByUserIdAndBookingId(userId:String, bookingId: String): PaymentAuthorization
    fun findByPaymentIntentId(paymentIntentId: String): PaymentAuthorization?
    fun insert(auth: PaymentAuthorization): PaymentAuthorization
    fun save(auth: PaymentAuthorization): PaymentAuthorization

    fun findByBookingIdOrderByRoleAscCreatedAtAsc(bookingId: String): List<PaymentAuthorization>


    @Query(PaymentAuthorizationQueries.CAPTURE_RETRY_CANDIDATES)
    fun findCaptureRetryCandidates(nowMillis: Long): List<PaymentAuthorization>
}