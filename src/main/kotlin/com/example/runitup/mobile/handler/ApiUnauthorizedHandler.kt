package com.example.runitup.mobile.handler

import com.example.runitup.mobile.exception.ApiUnauhorizedException
import com.example.runitup.mobile.rest.v1.dto.ApiError
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.ZoneId
import java.time.ZonedDateTime

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class ApiUnauthorizedHandler {
    @ExceptionHandler(value = [ApiUnauhorizedException::class])
    fun handleApiRequestException(e: ApiUnauhorizedException): ResponseEntity<Any> {
        val error = ApiError(
            e.message.orEmpty(),
            HttpStatus.BAD_REQUEST,
            ZonedDateTime.now(ZoneId.of("Z"))
        )
        return ResponseEntity(error, HttpStatus.UNAUTHORIZED)
    }
}