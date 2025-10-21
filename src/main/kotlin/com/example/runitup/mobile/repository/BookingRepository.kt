package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.BOOKING_COLLECTION)
interface BookingRepository : MongoRepository<com.example.runitup.mobile.model.Booking, String> {

    @Query("{runSessionId:'?0'}")
    fun findBySessionId(runSessionId: String): List<com.example.runitup.mobile.model.Booking>
    @Query("{userId:'?0'}")
    fun findByUserId(userId: String): List<com.example.runitup.mobile.model.Booking>
}