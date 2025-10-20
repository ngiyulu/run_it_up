package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDate

class Refund (
    @Id val id: String? = ObjectId().toString(),
    val userId: String,
    var runId: String?,
    var amount: Double,
    val status: com.example.runitup.mobile.model.RefundStatus = com.example.runitup.mobile.model.RefundStatus.PENDING,
    var reason: com.example.runitup.mobile.model.RefundReason = com.example.runitup.mobile.model.RefundReason.NONE,
    var requestedAt: LocalDate,
)
enum class RefundStatus{
    PENDING, COMPLETE
}
enum class RefundReason{
    USER_ACTION, CANCEL_JOB, NONE
}