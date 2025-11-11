package com.example.runitup.mobile.repository

import com.example.runitup.mobile.constants.CollectionConstants
import com.example.runitup.mobile.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Document(collection = CollectionConstants.USER_COLLECTION)
interface UserRepository : MongoRepository<User, String> {
    // existing
    @Query("{id:'?0'}") fun findByIdentifier(identifier: String): User?
    @Query("{email:'?0'}") fun findByEmail(email: String): User?
    fun findByLinkedAdmin(linkedAdmin: String): User?
    @Query("{phoneNumber:'?0'}") fun findByPhone(phoneNumber: String): User?
    @Query("{auth:'?0'}") fun findByAuth(auth: String): User?
    fun findAllByVerifiedPhone(verifiedPhone: Boolean, pageable: Pageable): Page<User>

    // 🔐 Auth / Stripe
    fun findByStripeId(stripeId: String): User?

    // 👤 Admin views
    fun findAllByLinkedAdminOrderByCreatedAtDesc(linkedAdmin: String, pageable: Pageable): Page<User>

    // ✅ Verification dashboards
    fun findAllByVerifiedPhoneOrderByCreatedAtDesc(verifiedPhone: Boolean, pageable: Pageable): Page<User>

    // 📧/📱 bulk lookups
    fun findByEmailIn(emails: Collection<String>): List<User>
    fun findByPhoneNumberIn(phones: Collection<String>): List<User>

    // 🧭 activity
    fun findAllByOrderByLoggedInAtDesc(pageable: Pageable): Page<User>

    // 🔎 optional search by name (use a text index below)
    // For text search, prefer a service that calls $text; you can also do:
    // @Query("{ \$text: { \$search: ?0 } }")
    // fun searchByNameOrDescription(q: String, pageable: Pageable): Page<User>
}
