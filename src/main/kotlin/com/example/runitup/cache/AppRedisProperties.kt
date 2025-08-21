package com.example.runitup.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties("app.redis")
data class AppRedisProperties(
    var host: String = "localhost",
    var username: String = "",
    var port: Int = 6379,
    var password: String? = null,
    var database: Int = 0,
    var ssl: Boolean = false,
    var timeout: Duration = Duration.ofSeconds(5),
    var cache: Cache = Cache()
) {
    data class Cache(
        var ttl: Map<String, Duration> = emptyMap() // keys: cache names; special key: "default"
    )
}