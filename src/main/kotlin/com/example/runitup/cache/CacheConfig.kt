package com.example.runitup.cache

import com.example.runitup.constants.CollectionConstants
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

//    @Bean
//    fun redisConnectionFactory(props: AppRedisProperties): RedisConnectionFactory {
////        val standalone = RedisStandaloneConfiguration().apply {
////            hostName = props.host
////            port = props.port
////            username = props.username
////            if (!props.password.isNullOrBlank()) setPassword(props.password!!)
////            database = props.database
////        }
//        val standalone = RedisStandaloneConfiguration(
//            "redis-17766.c245.us-east-1-3.ec2.redns.redis-cloud.com", 17766
//        ).apply {
//            username = "christian"                   // Redis Cloud ACL user
//            password = RedisPassword.of("b500d99387269f88b66ddfbb602ddae71db6de2ef006e8b66cbcd347b50e5fe6")
//            database = 0
//        }
//
//        val clientCfg = LettuceClientConfiguration.builder()
//            .commandTimeout(props.timeout)
//            .apply { if (props.ssl) useSsl() }
//            .build()
//
//        return LettuceConnectionFactory(standalone, clientCfg)
//    }

    @Bean
    fun kotlinObjectMapper(): ObjectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @Bean
    fun redisConnectionFactory(props: AppRedisProperties): LettuceConnectionFactory {
        val conf = RedisStandaloneConfiguration().apply {
            hostName = props.host
            port = props.port
            username = props.username
            password = RedisPassword.of(props.password)


        }

        return LettuceConnectionFactory(conf)
    }

    @Bean
    fun cacheManager(cf: RedisConnectionFactory, objectMapper: ObjectMapper): RedisCacheManager {
        val valueSerializer = GenericJackson2JsonRedisSerializer()
        val defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
            )
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(10)) // default TTL

        val perCache = mapOf(
            CollectionConstants.USER_COLLECTION to defaultCfg.entryTtl(Duration.ofMinutes(5000))
        )


        val config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
            )
        return RedisCacheManager.builder(cf)
            .cacheDefaults(config)
            .build()

//        return RedisCacheManager.builder(cf)
//            .cacheDefaults(defaultCfg)
//            .withInitialCacheConfigurations(perCache)
//            .build()
    }
}
