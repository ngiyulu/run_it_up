package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

data class Otp(
    @Id var id: String? = ObjectId().toString(),
    var code: String,
    var userId: String?,
    var phoneNumber: String,
    @Indexed(expireAfter = "0s", name = "otpCreatedAtIdx")
    val expiresAt: Instant = Instant.now().plus(10, ChronoUnit.MINUTES),
): BaseModel()