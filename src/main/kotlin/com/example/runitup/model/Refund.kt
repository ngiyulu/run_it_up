package com.example.runitup.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDate

class Refund (
    @Id val id: String? = ObjectId().toString(),
    val userId: String,
    var runId: String?,
    var amount: Double,
    val status:RefundStatus = RefundStatus.PENDING,
    var reason: RefundReason = RefundReason.NONE,
    var requestedAt: LocalDate,
)
enum class RefundStatus{
    PENDING, COMPLETE
}
enum class RefundReason{
    USER_ACTION, CANCEL_JOB, NONE
}