// service/RefundPolicyService.kt
package com.example.runitup.mobile.service.payment

import com.example.runitup.mobile.model.RunSession
import com.example.runitup.mobile.enum.RunStatus
import com.example.runitup.mobile.rest.v1.dto.RefundDecision
import org.springframework.stereotype.Service
import java.time.*


@Service
class RefundPolicyService {

    /**
     * Compute refund based on run status and time until start (run's local zone).
     *
     * @param run           The RunSession (uses date, startTime, zoneId, status)
     * @param chargeCents   The amount user actually paid/captured (¢). If you only authorized, pass 0.
     * @param now           Current time (UTC Instant). Default = Instant.now().
     */
    fun computeRefundForCancellation(
        run: RunSession,
        chargeCents: Long,
        now: Instant = Instant.now()
    ): RefundDecision {
        // Guard: nothing to refund
        if (chargeCents <= 0L) {
            return RefundDecision(
                eligible = false,
                percent = 0,
                refundCents = 0,
                reason = "No captured payment to refund."
            )
        }

        // Resolve start Instant in the run's zone
        val zone = try {
            ZoneId.of(run.zoneId)
        } catch (_: Exception) {
            // Fallback to UTC if zone invalid/missing
            ZoneOffset.UTC
        }
        val startZoned = ZonedDateTime.of(run.date, run.startTime, zone)
        val startAt = startZoned.toInstant()

        // Hours until start (can be negative if past start)
        val secondsUntil = Duration.between(now, startAt).seconds
        val hoursUntil = secondsUntil / 3600.0

        // 1) Non-refundable statuses
        when (run.status) {
            RunStatus.CONFIRMED -> {
                return RefundDecision(
                    eligible = false, percent = 0, refundCents = 0,
                    reason = "Run is CONFIRMED (≤ 1h to start). 0% refund by policy."
                )
            }
            RunStatus.COMPLETED, RunStatus.PROCESSED, RunStatus.ONGOING, RunStatus.CANCELLED -> {
                return RefundDecision(
                    eligible = false, percent = 0, refundCents = 0,
                    reason = "Run status ${run.status} is not refundable by policy."
                )
            }
            RunStatus.PENDING -> {
                // Continue below
            }
        }

        // 2) Refund tiers for PENDING
        // If less than 1 hour to start, you'd mark it CONFIRMED in your system. Treat as 0%.
        val percent = when {
            hoursUntil > 24.0 -> 100
            hoursUntil > 6.0  -> 75
            hoursUntil >= 1.0 -> 50
            else              -> 0 // < 1h
        }

        val refund = (chargeCents * percent) / 100L
        val reason = when (percent) {
            100 -> "Canceled > 24h before start. 100% refund."
            75  -> "Canceled 6–24h before start. 75% refund."
            50  -> "Canceled 1–6h before start. 50% refund."
            else -> "Canceled < 1h before start (or effectively CONFIRMED). 0% refund."
        }

        return RefundDecision(
            eligible = percent > 0,
            percent = percent,
            refundCents = refund,
            reason = reason
        )
    }
}
