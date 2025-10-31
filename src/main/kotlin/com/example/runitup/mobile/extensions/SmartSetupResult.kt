package com.example.runitup.mobile.extensions

import com.example.runitup.mobile.model.IntentState
import com.example.runitup.mobile.model.IntentStateType
import com.example.runitup.mobile.service.SmartSetupResult

fun SmartSetupResult.toIntentState(
    userId: String,
    sessionId: String,
    idempotencyKey: String? = null
): IntentState {
    return IntentState(
        userId = userId,
        sessionId = sessionId,
        intentType = IntentStateType.SETUP,
        intentId = this.setupIntentId ?: "unknown_intent",
        status = this.status ?: if (this.ok) "succeeded" else "requires_action",
        needsUserAction = this.needsUserAction,
        notifiedUserActionAt = null,
        idempotencyKey = idempotencyKey,
        lastErrorCode = this.errorCode,
        lastErrorMessage = this.errorMessage,
        expiresAt = null
    )
}