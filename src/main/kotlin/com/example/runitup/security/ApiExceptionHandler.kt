package com.example.runitup.security

import com.example.runitup.dto.ApiError
import com.example.runitup.exception.ApiRequestException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.ZoneId
import java.time.ZonedDateTime

@ControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(value = [ApiRequestException::class])
    fun handleApiRequestException(e: ApiRequestException): ResponseEntity<Any> {
        val error = ApiError(e.message.orEmpty(), HttpStatus.BAD_REQUEST, ZonedDateTime.now(ZoneId.of("Z")))
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }
}