package com.example.runitup.mobile.rest.v2.dto

import java.time.LocalDate

class MyBookingModel (val date: LocalDate, val sessions: List<com.example.runitup.mobile.model.RunSession>)