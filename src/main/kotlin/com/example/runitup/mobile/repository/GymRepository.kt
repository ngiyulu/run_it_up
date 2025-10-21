package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import org.springframework.data.geo.Distance
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.awt.Point

@Repository
@Document(collection = CollectionConstants.GYM_COLLECTION)
interface GymRepository : MongoRepository<com.example.runitup.mobile.model.Gym, String> {
    fun findByLocationNear(point: Point, distance: Distance): List<com.example.runitup.mobile.model.Gym>
}