package com.example.runitup.repository

import com.example.runitup.constants.CollectionConstants
import com.example.runitup.model.RunSession
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Point
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.SESSION_COLLECTION)
interface RunSessionRepository : MongoRepository<RunSession, String> {
    fun findByLocationNear(point: Point, distance: Distance): List<RunSession>
}