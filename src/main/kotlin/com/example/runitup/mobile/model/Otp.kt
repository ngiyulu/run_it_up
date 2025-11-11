package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant
import java.time.temporal.ChronoUnit

data class Otp(
    @Id var id: String? = ObjectId().toString(),
    var code: String,
    var userId: String?,
    var phoneNumber: String,
    val expiresAt: Instant = Instant.now().plus(10, ChronoUnit.MINUTES),
): BaseModel()