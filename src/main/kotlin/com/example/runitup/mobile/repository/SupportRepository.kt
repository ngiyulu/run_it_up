package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.Support
import com.example.runitup.mobile.model.SupportStatus
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.SUPPORT_COLLECTION)
interface SupportRepository : MongoRepository<Support, String> {

    // Basic filters
    fun findByStatus(status: SupportStatus): List<Support>
    fun findByEmail(email: String): List<Support>
    fun findByAdmin_Id(adminId: String): List<Support>  // if AdminUser.id is String

    // Time windows (UTC instants)
    fun findByStatusAndCreatedAtBetween(
        status: SupportStatus,
        startInclusive: java.time.Instant,
        endExclusive: java.time.Instant
    ): List<Support>

    fun findByCreatedAtBetween(
        startInclusive: java.time.Instant,
        endExclusive: java.time.Instant
    ): List<Support>

    // Resolution queries
    fun findByStatusAndResolvedAtIsNotNull(status: SupportStatus): List<Support>
    fun findByResolvedAt(resolvedAt: java.time.LocalDate): List<Support>

    // Paged versions for dashboards
    fun findByStatusOrderByCreatedAtDesc(
        status: SupportStatus,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Support>

    fun findByEmailOrderByCreatedAtDesc(
        email: String,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<Support>
}
