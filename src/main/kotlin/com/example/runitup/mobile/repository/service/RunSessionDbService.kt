package com.example.runitup.mobile.repository.service

import com.example.runitup.mobile.repository.RunSessionRepository
import com.example.runitup.mobile.service.TimeAndDateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RunSessionDbService {

    @Autowired
    lateinit var runSessionRepository: RunSessionRepository

    @Autowired
    lateinit var timeAndDateService: TimeAndDateService


}