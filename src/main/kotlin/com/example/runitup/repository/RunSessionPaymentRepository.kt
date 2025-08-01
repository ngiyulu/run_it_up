package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.RunSessionPayment
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.PAYMENT_COLLECTION)
interface RunSessionPaymentRepository: MongoRepository<RunSessionPayment, String> {


}