package com.example.runitup.common.repo

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Payment
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Document(collection = CollectionConstants.PAYMENT_COLLECTION)
@Repository
interface PaymentRepository : MongoRepository<Payment, String> {
    fun findAllBySessionId(sessionId: String): List<Payment>
    fun findAllByUserId(userId: String): List<Payment>
    fun findAllByCreatedAtBetween(start: Instant, end: Instant): List<Payment>
}