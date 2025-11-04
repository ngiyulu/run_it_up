package com.example.runitup.mobile.service

// service/RedisLockService.kt

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/*
Redis is a great distributed coordination system.
The idea is simple:

Before running a cron job, each instance tries to “acquire a lock” in Redis.

Only one instance succeeds.

Others see the lock exists and skip running.

After the job finishes, the lock expires or is released


If you have 10 servers, then all 10 will trigger runCleanupJob() every minute.

That’s fine for stateless work (like refreshing caches), but disastrous for jobs that:

Send emails or push notifications

Capture payments

Clean expired data

Generate reports

You’d end up with 10 duplicate executions — race conditions, double billing, etc.
 */
@Service
class RedisLockService(
    private val redis: StringRedisTemplate
) {
    /**
     * Try to acquire a lock for [lockKey] with TTL [ttlSeconds].
     * Returns a token if acquired (store it to release), or null if someone else holds it.
     */
    fun tryLock(lockKey: String, ttlSeconds: Long, token: String): Boolean {
        // SETNX + EXPIRE (opsForValue().setIfAbsent supports timeout)
        return redis.opsForValue().setIfAbsent(lockKey, token, ttlSeconds, TimeUnit.SECONDS) == true
    }

    /**
     * Release only if the token matches (prevents accidental unlock of others’ locks).
     */
    fun unlock(lockKey: String, token: String) {
        val cur = redis.opsForValue().get(lockKey)
        if (cur == token) {
            redis.delete(lockKey)
        }
    }
}
