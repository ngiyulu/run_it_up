package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.RunSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Point
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
@Document(collection = CollectionConstants.SESSION_COLLECTION)
interface RunSessionRepository : MongoRepository<RunSession, String> {
    fun findByLocationNear(point: Point, distance: Distance): List<RunSession>
    fun findAllByDate(date: LocalDate): List<RunSession>
    fun findAllByDateBetween(start: LocalDate, end: LocalDate): List<RunSession>

    @Query("{ 'date': { \$gte: ?0, \$lt: ?1 } }")
    fun findAllByDateBetween(startInclusive: Date, endExclusive: Date, pageable: Pageable): Page<RunSession>


    @Query("{ 'date': { \$eq: ?0} }")
    fun findAllByDatePageable(startInclusive: Date, pageable: Pageable): Page<RunSession>
}