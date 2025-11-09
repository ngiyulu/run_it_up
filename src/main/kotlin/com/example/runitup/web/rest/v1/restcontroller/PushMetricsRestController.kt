package com.example.runitup.web.rest.v1.restcontroller// package com.example.runitup.mobile.rest.v1.controllers.metrics

import com.example.runitup.mobile.model.*
import com.example.runitup.mobile.service.PushMetricsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/admin/api/v1/metrics/push")
class PushMetricsRestController(
    private val svc: PushMetricsService
) {

    private fun tr(
        from: Instant?,
        to: Instant?
    ): TimeRange {
        val end = to ?: Instant.now()
        val start = from ?: end.minusSeconds(7 * 24 * 3600) // default last 7 days
        return TimeRange(start, end)
    }

    // --- Overview ---
    @GetMapping("/overview")
    fun overview(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?
    ): OverviewMetrics = svc.overview(tr(from, to))

    // --- Breakdowns ---
    @GetMapping("/breakdown/templates")
    fun byTemplate(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?
    ): List<BreakdownRow> = svc.byTemplate(tr(from, to))

    @GetMapping("/breakdown/vendors")
    fun byVendor(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?
    ): List<BreakdownRow> = svc.byVendor(tr(from, to))

    @GetMapping("/breakdown/phone-types")
    fun byPhoneType(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?
    ): List<BreakdownRow> = svc.byPhoneType(tr(from, to))

    // --- Errors ---
    @GetMapping("/errors/top")
    fun topErrors(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): List<ErrorRow> = svc.topErrors(tr(from, to), limit)

    // --- Session-scoped (requires sessionId on attempts, or add $lookup) ---
    @GetMapping("/sessions/{sessionId}")
    fun sessionMetrics(
        @PathVariable sessionId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?
    ): SessionMetrics = svc.sessionMetrics(sessionId, tr(from, to))

    // --- User-scoped ---
    @GetMapping("/users/{userId}")
    fun userMetrics(
        @PathVariable userId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?
    ): UserMetrics = svc.userMetrics(userId, tr(from, to))
}
