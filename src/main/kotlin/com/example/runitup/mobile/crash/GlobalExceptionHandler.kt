package com.example.runitup.mobile.crash

// src/main/kotlin/com/example/error/GlobalExceptionHandler.kt

import com.example.runitup.mobile.service.myLogger
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.servlet.view.RedirectView

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GlobalExceptionHandler {

    private val logger = myLogger()

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException): RedirectView {
        return RedirectView("/")
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(ex: Throwable, req: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val traceId = MDC.get("traceId")
        // Log full stack
        logger.error("Unhandled error [traceId=$traceId] ${req.method} ${req.requestURI}", ex)
        val problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            title = "Internal Server Error"
            detail = ex.message ?: "Unexpected error"
            setProperty("traceId", traceId)
            setProperty("path", req.requestURI)
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }


}
