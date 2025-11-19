package com.example.runitup.utility

import com.example.runitup.mobile.utility.CaptureFailurePolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CaptureFailurePolicyTest {

    @Test
    fun `attempt 1 should retry with 30 seconds backoff`() {
        val result = CaptureFailurePolicy.decide(1)

        assertThat(result.shouldRetry).isTrue()
        assertThat(result.backoffMs).isEqualTo(30_000L)
    }

    @Test
    fun `attempt 2 should retry with 2 minutes backoff`() {
        val result = CaptureFailurePolicy.decide(2)

        assertThat(result.shouldRetry).isTrue()
        assertThat(result.backoffMs).isEqualTo(120_000L)
    }

    @Test
    fun `attempt 3 should NOT retry`() {
        val result = CaptureFailurePolicy.decide(3)

        assertThat(result.shouldRetry).isFalse()
        assertThat(result.backoffMs).isEqualTo(0L)
    }

    @Test
    fun `attempt 10 should NOT retry`() {
        val result = CaptureFailurePolicy.decide(10)

        assertThat(result.shouldRetry).isFalse()
        assertThat(result.backoffMs).isEqualTo(0L)
    }

    @Test
    fun `attempt zero or negative attempts should NOT retry`() {
        val zero = CaptureFailurePolicy.decide(0)
        val negative = CaptureFailurePolicy.decide(-1)

        assertThat(zero.shouldRetry).isFalse()
        assertThat(zero.backoffMs).isEqualTo(0L)

        assertThat(negative.shouldRetry).isFalse()
        assertThat(negative.backoffMs).isEqualTo(0L)
    }
}