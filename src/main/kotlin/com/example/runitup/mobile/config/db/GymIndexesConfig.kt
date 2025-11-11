package com.example.runitup.mobile.config.db
import com.example.runitup.mobile.model.Gym
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.GeospatialIndex

@Configuration
class GymIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureGymIndexes() {
        val idx = mongoTemplate.indexOps(Gym::class.java)

        // Geo
       // idx.createIndex(GeospatialIndex("location").named("location_2dsphere"))

        // Filters
        idx.createIndex(Index().on("city", Sort.Direction.ASC).named("city_idx"))
        idx.createIndex(Index().on("state", Sort.Direction.ASC).named("state_idx"))
        idx.createIndex(Index().on("zipCode", Sort.Direction.ASC).named("zip_idx"))

        // Optional: price and compound for typeahead
        idx.createIndex(Index().on("fee", Sort.Direction.ASC).named("fee_idx"))
        idx.createIndex(Index().on("city", Sort.Direction.ASC).on("title", Sort.Direction.ASC).named("city_title_idx"))

        // Optional: text search on title/description (only one text index per collection)
        // idx.createIndex(Index().text("title").text("description").named("text_title_description"))
    }
}
