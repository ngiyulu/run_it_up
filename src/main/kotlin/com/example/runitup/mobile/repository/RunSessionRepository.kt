package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.model.RunSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*


@Repository
@Document(collection = CollectionConstants.SESSION_COLLECTION)
interface RunSessionRepository : MongoRepository<RunSession, String> {

    companion object {
        // $near + status + UTC startAtUtc range + exclude user
        const val QUERY_NEAR_EXCLUDE_DAY_RANGE = """
{
  'location': {
    '${'$'}near': {
      '${'$'}geometry': { 'type': 'Point', 'coordinates': [ ?2, ?1 ] },
      '${'$'}maxDistance': ?3
    }
  },
  'status': { '${'$'}in': ?4 },
  'startAtUtc': { '${'$'}gte': ?5, '${'$'}lt': ?6 },
  'privateRun': false,
  'bookingList': { '${'$'}not': { '${'$'}elemMatch': { 'userId': ?0 } } },
  'waitList':   { '${'$'}not': { '${'$'}elemMatch': { 'userId': ?0 } } }
}
"""
    }
    fun findByStatus(status: RunStatus): List<RunSession>

    @Query("{id:'?0'}")
    fun findByIdentifier(identifier: String): RunSession?

    @Query("{ 'date': { \$gte: ?0, \$lt: ?1 } }")
    fun findAllByDateBetween(startInclusive: Date, endExclusive: Date, pageable: Pageable): Page<RunSession>

    @Query("{ 'date': { \$eq: ?0} }")
    fun findAllByDatePageable(startInclusive: Date, pageable: Pageable): Page<RunSession>

    @Query("{ 'date': { \$eq: ?0 }, 'hostedBy': { \$eq: ?1 } }")
    fun findAllByDateAndHostedBy(
        date: Date,
        hostedBy: String,
        pageable: Pageable
    ): Page<RunSession>

    @Query("{'date': { \$gte: ?0, \$lt: ?1 } }")
    fun findAllByDate( startInclusive: Date, endExclusive: Date,  pageable: Pageable): Page<RunSession>


    @Query(value = QUERY_NEAR_EXCLUDE_DAY_RANGE)
    fun findJoinableRunsExcludingUserNearOnLocalDay(
        userId: String,
        lat: Double,
        lng: Double,
        maxDistanceMeters: Double,
        statuses: List<RunStatus>,
        startInclusive: Date, endExclusive: Date,
        pageable: Pageable
    ): Page<RunSession>


}