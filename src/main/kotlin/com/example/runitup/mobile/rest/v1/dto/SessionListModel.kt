package com.example.runitup.mobile.rest.v1.dto

import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.ZoneId

class SessionListModel (
    val longitude: Double,
    val latitude: Double,
    val date: LocalDate,
    val pageRequest: PageRequest,
    var zoneId:ZoneId
)