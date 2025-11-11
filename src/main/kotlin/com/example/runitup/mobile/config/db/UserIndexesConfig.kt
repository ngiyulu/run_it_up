package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.User
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class UserIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureUserIndexes() {
        val idx = mongoTemplate.indexOps(User::class.java)

        // Uniques (sparse to avoid null duplicates)
        idx.createIndex(Index().on("email", Sort.Direction.ASC).unique().sparse().named("email_unique_idx"))
        idx.createIndex(Index().on("phoneNumber", Sort.Direction.ASC).unique().sparse().named("phone_unique_idx"))
       //we don't have an auth yet which is equivalent to the paassword
        // idx.createIndex(Index().on("auth", Sort.Direction.ASC).unique().sparse().named("auth_unique_idx"))

        // Stripe
        idx.createIndex(Index().on("stripeId", Sort.Direction.ASC).named("stripeId_idx"))

        // Dashboards / lists
        idx.createIndex(Index().on("linkedAdmin", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("linkedAdmin_createdAt_desc_idx"))
        idx.createIndex(Index().on("verifiedPhone", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC)
            .named("verifiedPhone_createdAt_desc_idx"))

        // Activity & generic time
        idx.createIndex(Index().on("loggedInAt", Sort.Direction.DESC).named("loggedInAt_desc_idx"))
        idx.createIndex(Index().on("createdAt", Sort.Direction.DESC).named("createdAt_desc_idx"))

        // Optional: text search on first/last name
        // idx.createIndex(Index().text("firstName").text("lastName").named("text_first_last"))
    }
}
