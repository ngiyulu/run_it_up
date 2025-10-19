package com.example.runitup.mobile.crash

// src/main/kotlin/com/example/error/GlobalExceptionHandler.kt

import com.example.runitup.mobile.exception.ApiRequestException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(ex: Throwable, req: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val traceId = MDC.get("traceId")
        // Log full stack
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
            .error("Unhandled error [traceId=$traceId] ${req.method} ${req.requestURI}", ex)

        if(ex is ApiRequestException){

            val problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
                setProperty("status", traceId)
                setProperty("message", ex.message.orEmpty())
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
        }
        val problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            title = "Internal Server Error"
            detail = ex.message ?: "Unexpected error"
            setProperty("traceId", traceId)
            setProperty("path", req.requestURI)
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }
}
