package com.example.runitup.mobile.model

import org.springframework.data.annotation.Id

data class WaitlistSetupState(
    @Id val id: String? = null,

    val bookingId: String,
    val sessionId: String,
    val userId: String,
    val customerId: String,
    val paymentMethodId: String,

    val setupIntentId: String? = null,

    var status: SetupStatus = SetupStatus.UNKNOWN,
    var needsUserAction: Boolean = false,
    var clientSecret: String? = null,        // only if action required
    var errorCode: String? = null,
    var errorMessage: String? = null,

    var lastNotifiedAt: Long? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
enum class SetupStatus { SUCCEEDED, REQUIRES_ACTION, CANCELED, ERROR, UNKNOWN }
