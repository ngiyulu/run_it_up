package com.example.runitup.mobile.service

import com.example.runitup.mobile.exception.ApiRequestException
import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.model.User
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.NearQuery
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class NearbyUserService(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * Finds users within [radiusMiles] of the runSession's gym location.
     *
     * Requirements:
     * - runSession.gym?.location must be non-null
     * - User.location must be GeoJsonPoint and indexed as 2dsphere
     */
    fun findUsersNearRunSession(runSession: RunSession, radiusMiles: Double): List<User> {
        val gymLocation = runSession.gym?.location
            ?: throw ApiRequestException("Gym location not found for session ${runSession.id}")

        // Mongo GeoJsonPoint stores (x=longitude, y=latitude)
        val center = Point(gymLocation.x, gymLocation.y)

        val nearQuery = NearQuery.near(center, Metrics.MILES)
            .maxDistance(Distance(radiusMiles, Metrics.MILES))
            .spherical(true)

        // Optional extra filters:
        val baseQuery = Query().addCriteria(
            Criteria.where("coordinate").ne(null)   // only users with location
        )

        // geoNear returns GeoResults<User>, we map to content
        val results = mongoTemplate.geoNear(nearQuery.query(baseQuery), User::class.java)

        return results.content.map { it.content }
    }
}