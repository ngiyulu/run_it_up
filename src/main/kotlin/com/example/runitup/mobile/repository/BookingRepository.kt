package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Booking
import com.example.runitup.mobile.model.RunSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
@Document(collection = CollectionConstants.BOOKING_COLLECTION)
interface BookingRepository : MongoRepository<Booking, String> {

    @Query("{runSessionId:'?0'}")
    fun findBySessionId(runSessionId: String): List<Booking>
    @Query("{userId:'?0'}")
    fun findByUserId(userId: String): List<Booking>

    @Query("{'createdAt': { \$gte: ?0, \$lte: ?1 } }")
    fun findAllByDateBetweenByUser( startInclusive: Date, endInclusive: Date,  pageable: Pageable): Page<Booking>

    @Query("{'createdAt': { \$eq: ?0} }")
    fun findByDate( date: Date, pageable: Pageable): Page<Booking>

}