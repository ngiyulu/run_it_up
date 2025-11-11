package com.example.runitup.mobile.model
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

data class PushNotificationEvent(
    @Id val id: String = ObjectId().toString(),

    // who/what triggered this send
    val trigger: String,               // e.g., "RUN_SESSION_CONFIRMED"
    val triggerRefId: String?,                  // e.g., runSessionId

    // de-dupe
    val dedupeKey: String?,                     // caller-supplied idempotency key (hash of template+scope+ids)

    // template & data snapshot (safe)
    val templateId: String,                     // e.g., "run.confirmed"
    val title: String?,
    val bodyPreview: String?,                   // optional: first N chars, or omit
    val dataKeys: Map<String, String>,          // e.g., { "screen": "RUN_DETAIL", "sessionId": "..." }

    // audience sizing
    val intendedCount: Int,
    val createdAt: Instant = Instant.now(),

    val expiresAt: Instant = Instant.now(),     // set to now; TTL is controlled by the index value above
)
