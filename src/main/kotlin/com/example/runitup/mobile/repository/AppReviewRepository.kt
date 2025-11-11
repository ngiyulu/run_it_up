package com.example.runitup.mobile.repository


import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.AppReview
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.REVIEW)
interface AppReviewRepository : MongoRepository<AppReview, String> {

    // Timelines
    fun findAllByOrderByCreatedAtDesc(pageable: org.springframework.data.domain.Pageable):
            org.springframework.data.domain.Page<AppReview>

    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<AppReview>

    // Ratings
    fun findByStar(star: Int, pageable: org.springframework.data.domain.Pageable):
            org.springframework.data.domain.Page<AppReview>

    fun findByStarBetween(minStar: Int, maxStar: Int, pageable: org.springframework.data.domain.Pageable):
            org.springframework.data.domain.Page<AppReview>

    // Counts (for dashboards)
    fun countByStar(star: Int): Long
    fun countByStarGreaterThanEqual(minStar: Int): Long

    // Optional: if you want “one active review per user” behavior
    // fun findFirstByUserIdOrderByCreatedAtDesc(userId: String): AppReview?
}
