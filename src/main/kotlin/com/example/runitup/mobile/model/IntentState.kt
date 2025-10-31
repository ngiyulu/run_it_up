package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id

data class IntentState(
    @Id var id: String? = ObjectId().toString(),
    val userId: String,
    val sessionId: String,
    val intentType: IntentStateType, // "SETUP" or "PAYMENT"
    val intentId: String,
    var status: String,
    var needsUserAction: Boolean,
    var notifiedUserActionAt: Long? = null,
    var idempotencyKey: String? = null,
    var lastErrorCode: String? = null,
    var lastErrorMessage: String? = null,
    var expiresAt: Long? = null
)
enum class IntentStateType{
    SETUP,
    PAYMENT
}