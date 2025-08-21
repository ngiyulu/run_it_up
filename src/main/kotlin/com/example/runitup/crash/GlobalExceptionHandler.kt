package com.example.runitup.crash

// src/main/kotlin/com/example/error/GlobalExceptionHandler.kt

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.ProblemDetail

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(ex: Throwable, req: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val traceId = MDC.get("traceId")
        // Log full stack
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
            .error("Unhandled error [traceId=$traceId] ${req.method} ${req.requestURI}", ex)

        val problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            title = "Internal Server Error"
            detail = ex.message ?: "Unexpected error"
            setProperty("traceId", traceId)
            setProperty("path", req.requestURI)
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }
}
