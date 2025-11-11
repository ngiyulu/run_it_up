package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Gym
import org.springframework.data.geo.Distance
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.awt.Point

@Repository
@Document(collection = CollectionConstants.GYM_COLLECTION)
interface GymRepository : MongoRepository<Gym, String> {

    // 🔎 Nearby gyms within a distance (uses 2dsphere index on `location`)
    fun findByLocationNear(point: Point, distance: Distance): List<Gym>

    // 🏙️ City/State filters
    fun findByCityIgnoreCase(city: String): List<Gym>
    fun findByStateIgnoreCase(state: String): List<Gym>
    fun findByZipCode(zipCode: String): List<Gym>

    // 🏷️ Name search
    fun findByTitleContainingIgnoreCase(titlePart: String): List<Gym>

    // 💵 Pricing filters
    fun findByFeeBetween(min: Double, max: Double): List<Gym>

    // 📍 Combined: city + title (for quick typeahead in a city)
    fun findByCityIgnoreCaseAndTitleContainingIgnoreCase(city: String, titlePart: String): List<Gym>
}
