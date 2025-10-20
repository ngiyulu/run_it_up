package com.example.runitup.mobile.clicksend

interface SmsService {

    suspend fun sendSmsDetailed(toE164: String, message: String, tag: String? = null): SendResult
    suspend fun fetchStatus(messageId: String): String
}