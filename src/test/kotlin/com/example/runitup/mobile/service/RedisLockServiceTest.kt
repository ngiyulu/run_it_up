// src/test/kotlin/com/example/runitup/mobile/service/RedisLockServiceTest.kt
package com.example.runitup.mobile.service

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.concurrent.TimeUnit

class RedisLockServiceTest {

    private val redis = mockk<StringRedisTemplate>()
    private val valueOps = mockk<ValueOperations<String, String>>()
    private lateinit var service: RedisLockService

    @BeforeEach
    fun setUp() {
        every { redis.opsForValue() } returns valueOps
        service = RedisLockService(redis)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `tryLock returns true when setIfAbsent succeeds`() {
        val key = "lock:cleanup"
        val token = "abc123"
        every { valueOps.setIfAbsent(key, token, 30, TimeUnit.SECONDS) } returns true

        val ok = service.tryLock(key, 30, token)

        assertThat(ok).isTrue()
        verify(exactly = 1) { valueOps.setIfAbsent(key, token, 30, TimeUnit.SECONDS) }
    }

    @Test
    fun `tryLock returns false when setIfAbsent fails`() {
        val key = "lock:report"
        val token = "t-1"
        every { valueOps.setIfAbsent(key, token, 10, TimeUnit.SECONDS) } returns false

        val ok = service.tryLock(key, 10, token)

        assertThat(ok).isFalse()
        verify(exactly = 1) { valueOps.setIfAbsent(key, token, 10, TimeUnit.SECONDS) }
    }

    @Test
    fun `tryLock returns false when setIfAbsent returns null (defensive)`() {
        val key = "lock:payments"
        val token = "t-2"
        every { valueOps.setIfAbsent(key, token, 5, TimeUnit.SECONDS) } returns null

        val ok = service.tryLock(key, 5, token)

        assertThat(ok).isFalse()
        verify(exactly = 1) { valueOps.setIfAbsent(key, token, 5, TimeUnit.SECONDS) }
    }

    @Test
    fun `unlock deletes key only when token matches`() {
        val key = "lock:cleanup"
        val token = "match-me"

        every { valueOps.get(key) } returns token
        every { redis.delete(key) } returns true

        service.unlock(key, token)

        verify(exactly = 1) { valueOps.get(key) }
        // Typed overload of delete(String) to avoid ambiguity with delete(Collection)
        verify(exactly = 1) { redis.delete(key) }
    }

    @Test
    fun `unlock does nothing when token does not match`() {
        val key = "lock:cleanup"
        every { valueOps.get(key) } returns "someone-else"

        service.unlock(key, "mine")

        verify(exactly = 1) { valueOps.get(key) }
        verify(exactly = 0) { redis.delete(any<String>()) }
        verify(exactly = 0) { redis.delete(any<Collection<String>>()) }
    }

    @Test
    fun `unlock does nothing when key does not exist`() {
        val key = "lock:cleanup"
        every { valueOps.get(key) } returns null

        service.unlock(key, "whatever")

        verify(exactly = 1) { valueOps.get(key) }
        verify(exactly = 0) { redis.delete(any<String>()) }
    }
}
