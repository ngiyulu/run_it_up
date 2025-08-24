package com.example.runitup.web.rest.v2.dto

import com.example.runitup.model.RunSession
import java.time.LocalDate

class MyBookingModel (val date: LocalDate, val sessions: List<RunSession>)