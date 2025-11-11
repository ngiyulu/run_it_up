package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant

data class RefundRecord(
    @Id var id: String? = ObjectId().toString(),

    var bookingId: String,
    var userId: String,
    var customerId: String,
    var currency: String,

    // Stripe linkage
    var paymentIntentId: String,
    var stripeRefundId: String? = null,

    // Money
    var amountCents: Long,
    var status: RefundStatus = RefundStatus.REQUESTED,

    // Reasoning
    var reasonCode: RefundReasonCode = RefundReasonCode.OTHER,
    var reasonMessage: String? = null,

    // Error capture
    var errorType: String? = null,
    var errorCode: String? = null,
    var declineCode: String? = null,
    var errorMessage: String? = null,

    // Audit
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
    var idempotencyKey: String
)

enum class RefundStatus { REQUESTED, SUCCEEDED, FAILED }
enum class RefundReasonCode {
    USER_CANCELLED,           // user-initiated cancel (eligible per policy)
    HOST_CANCELLED,           // session canceled by host
    PARTIAL_ADJUSTMENT,       // admin/manual adjustment
    DUPLICATE,                // duplicate payment
    FRAUDULENT,               // suspected fraudulent
    OTHER
}