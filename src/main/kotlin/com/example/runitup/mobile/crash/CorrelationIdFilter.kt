package com.example.runitup.mobile.crash


import com.example.runitup.mobile.constants.AppConstant
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/*
That’s a Spring OncePerRequestFilter that automatically adds and propagates a trace or correlation ID for each HTTP request — it’s part of a standard pattern for distributed tracing and log correlation.
Because it extends OncePerRequestFilter, this filter runs exactly once per request before your controllers or other filters handle it.

If the incoming request already has a header named X-Trace-Id, it reuses it.
→ This allows trace IDs to flow across services (e.g., from a frontend or API gateway).

Otherwise, it generates a new random UUID as the trace ID.
 */
@Component
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val traceId = req.getHeader("X-Trace-Id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        MDC.put(AppConstant.TRACE_ID, traceId)
        res.setHeader("X-Trace-Id", traceId)

        // ✅ capture the request source type (e.g., mobile, web, admin, etc.)
        val sourceType = req.getHeader(AppConstant.SOURCE)?.lowercase() ?: "unknown"
        MDC.put(AppConstant.SOURCE, sourceType)

        // optional: echo it back for debugging or propagation
        res.setHeader(AppConstant.SOURCE, sourceType)

        try { chain.doFilter(req, res) } finally { MDC.remove("traceId") }
    }
}
