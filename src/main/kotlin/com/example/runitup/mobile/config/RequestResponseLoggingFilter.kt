package com.example.runitup.mobile.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.StandardCharsets

@Configuration
@Order(100) // run after Spring security etc., adjust as needed
class RequestResponseLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Wrap so we can read bodies after the request is processed
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val start = System.nanoTime()
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000

            val method = request.method
            val uri = request.requestURI + (request.queryString?.let { "?$it" } ?: "")
            val status = wrappedResponse.status

            val reqBody = bodyAsString(wrappedRequest.contentAsByteArray, request.characterEncoding)
            // If you also want response body, uncomment:
            // val resBody = bodyAsString(wrappedResponse.contentAsByteArray, response.characterEncoding)

            // Truncate to avoid huge logs; tweak limit as needed
            val truncatedReq = truncate(reqBody, 2000)

            log.info(
                "[{} {}] -> {} in {} ms | body={}",
                method, uri, status, durationMs, truncatedReq
            )

            // IMPORTANT: write cached response body back to the real response
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length <= max) s else s.take(max) + "...(${s.length} chars)"

    private fun bodyAsString(bytes: ByteArray, encoding: String?): String =
        if (bytes.isEmpty()) "" else try { String(bytes, StandardCharsets.UTF_8) } catch (_: Exception) { "<unreadable>" }
}
