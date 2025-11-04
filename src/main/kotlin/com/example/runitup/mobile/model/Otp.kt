package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant
import java.util.*

data class Otp(
    @Id var id: String? = ObjectId().toString(),
    var code: String,
    var userId: String?,
    var phoneNumber: String,
    // createDate indexed for automatic expiration 5 minutes after insertion
    @Indexed(name = "otpCreatedAtIdx", expireAfter = "5m")
    var created: Date? = Date.from(Instant.now())
): BaseModel()