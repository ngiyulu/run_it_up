package com.example.runitup.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDate

class Refund (
    @Id val id: ObjectId? = ObjectId(),
    val userId: String,
    var runId: String?,
    var amount: Double,
    val status:RefundStatus = RefundStatus.PENDING,
    var requestedAt: LocalDate,
)
enum class RefundStatus{
    PENDING, COMPLETE
}