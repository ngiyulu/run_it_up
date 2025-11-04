package com.example.runitup.mobile.crash


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
        MDC.put("traceId", traceId)
        res.setHeader("X-Trace-Id", traceId)
        try { chain.doFilter(req, res) } finally { MDC.remove("traceId") }
    }
}
