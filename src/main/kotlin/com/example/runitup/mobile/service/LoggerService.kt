package com.example.runitup.mobile.service

import org.springframework.stereotype.Service

@Service
class LoggerService{
//    private val log = LoggerFactory.getLogger(javaClass)
    fun logInfo(tag: String, data: Any?){
//        log.info(tag, data)
    }

    fun logError(tag: String, data: Any?){
//        log.error(tag, data)
    }

    fun logWarn(tag: String, data: Any?){
//        log.warn(tag, data)
    }
}