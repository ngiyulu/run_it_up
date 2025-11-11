package com.example.runitup.mobile.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

enum class PushVendor { FCM, APNS }
enum class AttemptStatus { SUCCESS, FAILED }

// If using @Document on PushDeliveryAttempt, you can add:
// @CompoundIndexes(
//   CompoundIndex(name="reqAt_idx", def="{ 'requestedAt': 1 }"),
//   CompoundIndex(name="template_req_idx", def="{ 'templateId': 1, 'requestedAt': 1 }"),
//   CompoundIndex(name="vendor_req_idx", def="{ 'vendor': 1, 'requestedAt': 1 }"),
//   CompoundIndex(name="phone_req_idx", def="{ 'phoneType': 1, 'requestedAt': 1 }"),
//   CompoundIndex(name="session_req_idx", def="{ 'sessionId': 1, 'requestedAt': 1 }"),
//   CompoundIndex(name="user_req_idx", def="{ 'userId': 1, 'requestedAt': 1 }")
// )

data class PushDeliveryAttempt(
    @Id val id: String = ObjectId().toString(),
// in PushDeliveryAttempt model
    val templateId: String?,   // e.g. "run.confirmed"
    val sessionId: String?,    // runSession.id (if applicable)
    val eventId: String,
    val userId: String,                // sender-side user
    val phoneId: String?,
    val tokenHash: String,                      // never store raw token; use SHA-256
    val phoneType: String,                      // "ANDROID" | "IOS"
    val vendor: PushVendor,

    val requestedAt: Instant,
    val completedAt: Instant,
    val status: AttemptStatus,

    // response mapping
    val vendorMessageId: String?,               // FCM/APNs id if available
    val errorCode: String?,                     // standardize (e.g., "NotRegistered", "InvalidRegistration", "Unregistered", "DeviceTokenNotForTopic")
    val errorMessage: String?,                  // sanitized
    val expiresAt: Instant = Instant.now()
)
