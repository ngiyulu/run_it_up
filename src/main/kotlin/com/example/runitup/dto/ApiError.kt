package com.example.runitup.dto

import org.springframework.http.HttpStatus
import java.time.ZonedDateTime

class ApiError(val message:String,
               val  status: HttpStatus,
               val timestamp: ZonedDateTime
) {
}