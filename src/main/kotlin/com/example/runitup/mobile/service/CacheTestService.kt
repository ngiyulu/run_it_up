package com.example.runitup.mobile.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class CacheTestService(
    private val redis: StringRedisTemplate
) {
    fun ping(): String = redis.connectionFactory?.connection?.ping() ?: "NO_CONNECTION"

    fun set(key: String, value: String) {
        redis.opsForValue().set(key, value)
    }

    fun get(key: String): String? = redis.opsForValue().get(key)
}