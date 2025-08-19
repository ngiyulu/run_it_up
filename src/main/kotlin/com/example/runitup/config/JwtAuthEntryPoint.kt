package com.example.runitup.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtAuthEntryPoint : AuthenticationEntryPoint {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val authHeader = request.getHeader("Authorization")
        log.warn(
            "401 Unauthorized (invalid/missing JWT) -> method={}, path={}, header={}, reason={}, authHeader={}",
            request.method, request.requestURI, request.headerNames, authException.message,
            if ((authHeader?.length ?: 0) > 20) authHeader?.take(20) + "..." else authHeader
        )
        printAllHeaders(request)
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized")
    }

    fun printAllHeaders(req: HttpServletRequest) {
        val names = Collections.list(req.headerNames)
        for (name in names) {
            val values = Collections.list(req.getHeaders(name))
            println("$name: ${values.joinToString(", ")}")
        }
    }
}
