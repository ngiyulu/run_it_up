package com.example.runitup.mobile.service

import org.springframework.beans.factory.annotation.Autowired

abstract class BaseService {
    val TAG = this::class.java.simpleName

    @Autowired
    lateinit var logger: com.example.runitup.mobile.service.LoggerService


}