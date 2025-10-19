package com.example.runitup.mobile.cache

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@Configuration
class RedisSmokeTestConfig {

    @Bean
    fun redisSmokeTest(cf: RedisConnectionFactory) = CommandLineRunner {
        // Low-level PING
        val conn = cf.connection
        try {
            val pong = conn.ping()
            println("Redis PING -> $pong") // Expect "PONG"
        } finally {
            conn.close()
        }

        // High-level SET/GET with TTL
        val tmpl = StringRedisTemplate(cf)
        val key = "boot:ping"
        tmpl.opsForValue().set(key, "ok", Duration.ofSeconds(30))
        val got = tmpl.opsForValue().get(key)
        check(got == "ok") { "Unexpected value from Redis: $got" }
        val ttl = tmpl.getExpire(key)
        println("Redis SET/GET ok; TTL=$ttl s")
    }
}
