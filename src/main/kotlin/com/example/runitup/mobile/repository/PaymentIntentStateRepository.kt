package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.IntentState
import com.example.runitup.mobile.model.Refund
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PAYMENT_STATE_COLLECTION)
interface PaymentIntentStateRepository : MongoRepository<IntentState, String> {}