package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.Refund
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.REFUND_COLLECTION)
interface RefundRepository : MongoRepository<Refund, String> {}