package com.example.runitup.mobile.config.db

import com.example.runitup.mobile.model.Otp
import jakarta.annotation.PostConstruct
import org.bson.Document
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria

@Configuration
class OtpIndexesConfig(private val mongoTemplate: MongoTemplate) {

    @PostConstruct
    fun ensureOtpIndexes() {
        val idx = mongoTemplate.indexOps(Otp::class.java)

        // 1️⃣ TTL (expires 10 minutes after `expiresAt`)
        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("expiresAt" to 1)))
                .named("otp_expiry_idx")
                .expire(600) // <-- 600 seconds = 10 minutes
        )

        // 2️⃣ Lookup by phone number + active flag
        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("phoneNumber" to 1, "isActive" to 1)))
                .named("phone_isActive_idx")
        )

        // 3️⃣ Lookup by userId
        idx.createIndex(
            CompoundIndexDefinition(Document(mapOf("userId" to 1)))
                .named("userId_idx")
        )

        // 4️⃣ Optional: enforce single active OTP per phone
        idx.createIndex(
            Index()
                .on("phoneNumber", Sort.Direction.ASC)
                .named("unique_active_phone_idx")
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("isActive").`is`(true)))
        )
    }
}
