package com.example.runitup.mobile.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed

data class WaitlistSetupState(
    @Id val id: String? = null,

    @Indexed val bookingId: String,
    @Indexed val sessionId: String,
    @Indexed val userId: String,
    @Indexed val customerId: String,
    @Indexed val paymentMethodId: String,

    @Indexed(unique = true) val setupIntentId: String? = null,

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
