package com.example.runitup.common.service

import org.springframework.stereotype.Service
import java.util.*

@Service
class IdempotencyKeyGenerator {

    fun generateIdempotencyKey(prefix: String, userId: String, sessionId: String): String {
        return "$prefix-$sessionId-$userId-${UUID.randomUUID()}"
    }
}