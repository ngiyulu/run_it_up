package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.Booking
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.BOOKING_COLLECTION)
interface BookingRepository : MongoRepository<Booking, String> {

    @Query("{runSessionId:'?0'}")
    fun findBySessionId(runSessionId: String): List<Booking>
    @Query("{userId:'?0'}")
    fun findByUserId(userId: String): List<Booking>
}