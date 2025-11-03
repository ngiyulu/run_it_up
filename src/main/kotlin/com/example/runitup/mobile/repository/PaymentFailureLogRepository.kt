package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.PaymentFailureLog
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PAYMENT_FAILURE_COLLECTION)
interface PaymentFailureLogRepository : MongoRepository<PaymentFailureLog, String> {
}