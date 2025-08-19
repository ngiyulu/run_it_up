package com.example.runitup.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class LoggingAccessDeniedHandler : AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        ex: AccessDeniedException
    ) {
        println(request.headerNames)
        log.warn(
            "403 Forbidden -> method={}, path={}, user={}, ip={}, reason={}",
            request.method,
            request.requestURI,
            request.userPrincipal?.name ?: "anonymous",
            request.remoteAddr,
            ex.message
        )
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
    }
}
