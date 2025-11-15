package com.example.runitup.mobile.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties("app.redis.cache")
data class CacheRedisProperties(
    var host: String = "localhost",
    var username: String = "",
    var port: Int = 6379,
    var password: String? = null,
    var database: Int = 0,
    var ssl: Boolean = false,
    var timeout: Duration = Duration.ofSeconds(5),
)

@Configuration
@ConfigurationProperties("app.redis.queue")
data class QueueRedisProperties(
    var host: String = "localhost",
    var username: String = "",
    var port: Int = 6379,
    var password: String? = null,
    var database: Int = 1,
    var ssl: Boolean = false,
    var timeout: Duration = Duration.ofSeconds(5),
)
