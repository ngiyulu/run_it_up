package com.example.runitup.mobile.cache

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@Configuration
class RedisSmokeTestConfig {

    @Bean
    fun redisSmokeTest(
        @Qualifier("cacheRedisConnectionFactory")
        cacheCf: RedisConnectionFactory,

        @Qualifier("queueRedisConnectionFactory")
        queueCf: RedisConnectionFactory
    ) = CommandLineRunner {

        fun testRedis(name: String, cf: RedisConnectionFactory) {
            println("🔍 Running Redis smoke test for: $name")

            // Low-level PING
            val conn = cf.connection
            try {
                val pong = conn.ping()
                println("[$name] Redis PING -> $pong") // Expect "PONG"
            } finally {
                conn.close()
            }

            // High-level SET/GET with TTL
            val tmpl = StringRedisTemplate(cf)
            val key = "boot:ping:$name"
            tmpl.opsForValue().set(key, "ok", Duration.ofSeconds(30))
            val got = tmpl.opsForValue().get(key)
            check(got == "ok") { "[$name] Unexpected value from Redis: $got" }
            val ttl = tmpl.getExpire(key)
            println("[$name] Redis SET/GET ok; TTL=$ttl s")
        }

        // Run tests for both Redis setups
        testRedis("cache", cacheCf)
        testRedis("queue", queueCf)
    }
}
