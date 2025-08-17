package com.example.runitup.service

import org.springframework.beans.factory.annotation.Autowired

abstract class BaseService {
    val TAG = this::class.java.simpleName

    @Autowired
    lateinit var logger: LoggerService


}