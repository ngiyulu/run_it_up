package com.example.runitup.web.rest.v1.dto

import org.springframework.http.HttpStatus
import java.time.ZonedDateTime

class ApiError(val message:String,
               val  status: HttpStatus,
               val timestamp: ZonedDateTime
) {
}