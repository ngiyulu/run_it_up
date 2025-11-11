package com.example.runitup.mobile.service


import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

enum class ActionType {
    PAYMENT_SCA,          // Complete 3DS for SetupIntent/PaymentIntent
    PAYMENT_UPDATE_CARD,  // Add/choose a new card
    BOOKING_ATTENTION,    // Booking-specific attention required
    GENERIC               // Fallback
}

enum class ActionStatus { PENDING, SNOOZED, RESOLVED, EXPIRED }
enum class ActionSource { SYSTEM, STRIPE_WEBHOOK, JOB, ADMIN }

@Document("user_action_required")
data class UserActionRequired(
    @Id val id: String? = null,

    val userId: String,
    val type: ActionType,
    val dedupeKey: String, // e.g., "PAYMENT_SCA:seti_123" or "PAYMENT_SCA:pi_123"

    // UX
    var title: String,
    var message: String,          // user-facing; keep it short & clear
    var ctaLabel: String = "Fix now",
    var deepLink: String? = null, // e.g., "runitup://payment/sca?si=seti_123"

    // State
    var status: ActionStatus = ActionStatus.PENDING,
    var priority: Int = 100,      // lower = higher priority
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var expiresAt: Long? = null,  // optional expiry to auto-hide stale items
    var snoozedUntil: Long? = null,

    // Data the client/backend needs to handle the action.
    // Keep secrets OUT (do not store client_secret here).
    var metadata: Map<String, String> = emptyMap(),

    // Traceability
    var source: ActionSource = ActionSource.SYSTEM
)
