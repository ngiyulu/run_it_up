package com.example.runitup.mobile.cache

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class CacheConfig {

    /** Make this the primary mapper used by MVC, WebFlux, etc. */
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    // --- CACHE Redis (for Spring @Cacheable) ---
    @Bean("cacheRedisConnectionFactory")
    @Primary
    fun cacheRedisConnectionFactory(props: CacheRedisProperties): LettuceConnectionFactory {
        val conf = RedisStandaloneConfiguration().apply {
            hostName = props.host
            port = props.port
            username = props.username
            if (!props.password.isNullOrBlank()) {
                password = RedisPassword.of(props.password)
            }
            database = props.database       // e.g. 0 for dev
        }
        return LettuceConnectionFactory(conf)
    }

    // --- QUEUE Redis (for LightSqsService) ---
    @Bean("queueRedisConnectionFactory")
    fun queueRedisConnectionFactory(props: QueueRedisProperties): LettuceConnectionFactory {
        val conf = RedisStandaloneConfiguration().apply {
            hostName = props.host
            port = props.port
            username = props.username
            if (!props.password.isNullOrBlank()) {
                password = RedisPassword.of(props.password)
            }
            database = props.database       // can also be 0 in dev, different in prod
        }
        return LettuceConnectionFactory(conf)
    }

    // --- CacheManager uses CACHE Redis ---
    @Bean
    fun cacheManager(
        @Qualifier("cacheRedisConnectionFactory") cf: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisCacheManager {
        val valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper)

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
            )
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(10))

        return RedisCacheManager.builder(cf)
            .cacheDefaults(config)
            .build()
    }

    // --- StringRedisTemplate for QUEUE Redis ---
    @Bean("queueRedisTemplate")
    fun queueRedisTemplate(
        @Qualifier("queueRedisConnectionFactory") cf: RedisConnectionFactory
    ): StringRedisTemplate = StringRedisTemplate(cf)
}
