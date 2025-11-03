// service/UserActionService.kt
package com.example.runitup.mobile.service

import com.example.runitup.mobile.repository.UserActionRequiredRepository
import org.springframework.stereotype.Service

@Service
class UserActionService(
    private val repo: UserActionRequiredRepository
) {
    fun upsertPending(
        userId: String,
        type: ActionType,
        dedupeKey: String,
        title: String,
        message: String,
        ctaLabel: String = "Fix now",
        deepLink: String? = null,
        priority: Int = 100,
        expiresAt: Long? = null,
        metadata: Map<String, String> = emptyMap(),
        source: ActionSource = ActionSource.SYSTEM
    ): UserActionRequired {
        val now = System.currentTimeMillis()
        val existing = repo.findByUserIdAndDedupeKey(userId, dedupeKey)
        val row = existing?.apply {
            this.title = title
            this.message = message
            this.ctaLabel = ctaLabel
            this.deepLink = deepLink
            this.priority = priority
            this.expiresAt = expiresAt
            this.metadata = metadata
            this.status = if (this.status == ActionStatus.RESOLVED) ActionStatus.PENDING else this.status
            this.updatedAt = now
        }
            ?: UserActionRequired(
                userId = userId,
                type = type,
                dedupeKey = dedupeKey,
                title = title,
                message = message,
                ctaLabel = ctaLabel,
                deepLink = deepLink,
                status = ActionStatus.PENDING,
                priority = priority,
                expiresAt = expiresAt,
                metadata = metadata,
                source = source
            )
        return if (existing == null) repo.insert(row) else repo.save(row)
    }

    fun resolve(id: String, userId: String) {
        val row = repo.findById(id).orElse(null) ?: return
        if (row.userId != userId) return
        row.status = ActionStatus.RESOLVED
        row.updatedAt = System.currentTimeMillis()
        repo.save(row)
    }

    fun resolveByKey(userId: String, dedupeKey: String) {
        val row = repo.findByUserIdAndDedupeKey(userId, dedupeKey) ?: return
        row.status = ActionStatus.RESOLVED
        row.updatedAt = System.currentTimeMillis()
        repo.save(row)
    }

    fun snooze(id: String, userId: String, snoozeMinutes: Long = 60) {
        val row = repo.findById(id).orElse(null) ?: return
        if (row.userId != userId) return
        val now = System.currentTimeMillis()
        row.status = ActionStatus.SNOOZED
        row.snoozedUntil = now + snoozeMinutes * 60_000
        row.updatedAt = now
        repo.save(row)
    }

    fun pendingForLaunch(userId: String, max: Int = 5): List<UserActionRequired> {
        val now = System.currentTimeMillis()
        return repo.findByUserIdAndStatusInOrderByPriorityAscCreatedAtAsc(
            userId,
            listOf(ActionStatus.PENDING, ActionStatus.SNOOZED)
        ).filter { r ->
            val notExpired = r.expiresAt?.let { it > now } ?: true
            val notSnoozed = r.snoozedUntil?.let { it <= now } ?: true
            notExpired && notSnoozed
        }.take(max)
    }
}
