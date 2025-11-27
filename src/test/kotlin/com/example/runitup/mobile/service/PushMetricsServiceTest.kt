package com.example.runitup.mobile.service

import com.example.runitup.mobile.model.*
import org.bson.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import java.time.Instant

class PushMetricsServiceTest {

    private val mongo: MongoTemplate = mock()
    private val service = PushMetricsService(mongo)

    private fun aggResults(vararg docs: Document): AggregationResults<Document> =
        AggregationResults(docs.toList(), Document())

    // --- overview() ---

    @Test
    fun `overview returns metrics from aggregation result`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        val doc = Document().apply {
            put("attempts", 10)
            put("success", 8)
            put("failed", 2)
            put("invalidTokens", 3)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults(doc))

        val result = service.overview(range)

        assertEquals(10, result.attempts)
        assertEquals(8, result.success)
        assertEquals(2, result.failed)
        assertEquals(3, result.invalidTokenCount)
        // successRate = 8/10 * 100 = 80.0, rounded to 1 decimal
        assertEquals(80.0, result.successRate)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    @Test
    fun `overview returns zeros when aggregation result is empty`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults()) // no docs

        val result = service.overview(range)

        assertEquals(0, result.attempts)
        assertEquals(0, result.success)
        assertEquals(0, result.failed)
        assertEquals(0, result.invalidTokenCount)
        assertEquals(0.0, result.successRate)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    // --- breakdownByField() and wrappers ---

    @Test
    fun `breakdownByField maps documents to BreakdownRow with successRate`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        val doc1 = Document().apply {
            put("_id", "templateA")
            put("attempts", 10)
            put("success", 7)
            put("failed", 3)
        }
        val doc2 = Document().apply {
            put("_id", "templateB")
            put("attempts", 5)
            put("success", 1)
            put("failed", 4)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults(doc1, doc2))

        val rows = service.breakdownByField(range, "templateId")

        assertEquals(2, rows.size)

        val r1 = rows[0]
        assertEquals("templateA", r1.key)
        assertEquals(10, r1.attempts)
        assertEquals(7, r1.success)
        assertEquals(3, r1.failed)
        // successRate = 7/10 * 100 = 70.0
        assertEquals(70.0, r1.successRate)

        val r2 = rows[1]
        assertEquals("templateB", r2.key)
        assertEquals(5, r2.attempts)
        assertEquals(1, r2.success)
        assertEquals(4, r2.failed)
        // successRate = 1/5 * 100 = 20.0
        assertEquals(20.0, r2.successRate)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    @Test
    fun `breakdownByField uses UNKNOWN when _id is null`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        val doc = Document().apply {
            put("_id", null)
            put("attempts", 3)
            put("success", 2)
            put("failed", 1)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults(doc))

        val rows = service.breakdownByField(range, "phoneType")
        assertEquals(1, rows.size)
        assertEquals("UNKNOWN", rows[0].key)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    @Test
    fun `byTemplate delegates to breakdownByField under the hood`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        val doc = Document().apply {
            put("_id", "welcome.push")
            put("attempts", 4)
            put("success", 3)
            put("failed", 1)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults(doc))

        val rows = service.byTemplate(range)

        assertEquals(1, rows.size)
        assertEquals("welcome.push", rows[0].key)
        assertEquals(4, rows[0].attempts)
        assertEquals(3, rows[0].success)
        assertEquals(1, rows[0].failed)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    // --- topErrors() ---

    @Test
    fun `topErrors maps aggregation documents to ErrorRow`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        val e1 = Document().apply {
            put("_id", "InvalidToken")
            put("count", 5)
        }
        val e2 = Document().apply {
            put("_id", "NotRegistered")
            put("count", 2)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults(e1, e2))

        val errors = service.topErrors(range, limit = 10)

        assertEquals(2, errors.size)
        assertEquals("InvalidToken", errors[0].errorCode)
        assertEquals(5, errors[0].count)
        assertEquals("NotRegistered", errors[1].errorCode)
        assertEquals(2, errors[1].count)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    @Test
    fun `topErrors uses UNKNOWN when errorCode id is null`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)

        val e = Document().apply {
            put("_id", null)
            put("count", 3)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        ).thenReturn(aggResults(e))

        val errors = service.topErrors(range, limit = 5)

        assertEquals(1, errors.size)
        assertEquals("UNKNOWN", errors[0].errorCode)
        assertEquals(3, errors[0].count)

        verify(mongo).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    // --- sessionMetrics() ---

    @Test
    fun `sessionMetrics aggregates overview breakdown and errors`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)
        val sessionId = "session-123"

        // 1) overviewWithExtraMatch aggregation result
        val ovDoc = Document().apply {
            put("attempts", 6)
            put("success", 4)
            put("failed", 2)
            put("invalidTokens", 1)
        }

        // 2) breakdownWithExtraMatch result (by templateId)
        val brDoc = Document().apply {
            put("_id", "run.confirmed")
            put("attempts", 6)
            put("success", 4)
            put("failed", 2)
        }

        // 3) topErrorsWithExtraMatch result
        val errDoc = Document().apply {
            put("_id", "InvalidToken")
            put("count", 1)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        )
            .thenReturn(aggResults(ovDoc))     // overview
            .thenReturn(aggResults(brDoc))    // breakdown
            .thenReturn(aggResults(errDoc))   // errors

        val metrics = service.sessionMetrics(sessionId, range)

        assertEquals(sessionId, metrics.sessionId)

        // overview
        assertEquals(6, metrics.overview.attempts)
        assertEquals(4, metrics.overview.success)
        assertEquals(2, metrics.overview.failed)
        assertEquals(1, metrics.overview.invalidTokenCount)
        assertEquals(66.7, metrics.overview.successRate, 0.1) // 4/6 * 100 ~= 66.7

        // breakdown by template
        assertEquals(1, metrics.byTemplate.size)
        val br = metrics.byTemplate[0]
        assertEquals("run.confirmed", br.key)
        assertEquals(6, br.attempts)
        assertEquals(4, br.success)
        assertEquals(2, br.failed)

        // errors
        assertEquals(1, metrics.topErrors.size)
        val er = metrics.topErrors[0]
        assertEquals("InvalidToken", er.errorCode)
        assertEquals(1, er.count)

        // aggregate should have been invoked 3 times total
        verify(mongo, times(3)).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }

    // --- userMetrics() ---

    @Test
    fun `userMetrics aggregates overview breakdown and errors`() {
        val from = Instant.parse("2025-01-01T00:00:00Z")
        val to = Instant.parse("2025-01-02T00:00:00Z")
        val range = TimeRange(from, to)
        val userId = "user-999"

        // 1) overviewWithExtraMatch aggregation result
        val ovDoc = Document().apply {
            put("attempts", 3)
            put("success", 2)
            put("failed", 1)
            put("invalidTokens", 0)
        }

        // 2) breakdownWithExtraMatch result (by templateId)
        val brDoc = Document().apply {
            put("_id", "reminder.beforeRun")
            put("attempts", 3)
            put("success", 2)
            put("failed", 1)
        }

        // 3) topErrorsWithExtraMatch result
        val errDoc = Document().apply {
            put("_id", "NotRegistered")
            put("count", 1)
        }

        whenever(
            mongo.aggregate(
                any<Aggregation>(),
                eq(PushDeliveryAttempt::class.java),
                eq(Document::class.java)
            )
        )
            .thenReturn(aggResults(ovDoc))     // overview
            .thenReturn(aggResults(brDoc))    // breakdown
            .thenReturn(aggResults(errDoc))   // errors

        val metrics = service.userMetrics(userId, range)

        assertEquals(userId, metrics.userId)

        // overview
        assertEquals(3, metrics.overview.attempts)
        assertEquals(2, metrics.overview.success)
        assertEquals(1, metrics.overview.failed)
        assertEquals(0, metrics.overview.invalidTokenCount)
        assertEquals(66.7, metrics.overview.successRate, 0.1) // 2/3 * 100 ~= 66.7

        // breakdown by template
        assertEquals(1, metrics.byTemplate.size)
        val br = metrics.byTemplate[0]
        assertEquals("reminder.beforeRun", br.key)
        assertEquals(3, br.attempts)
        assertEquals(2, br.success)
        assertEquals(1, br.failed)

        // errors
        assertEquals(1, metrics.topErrors.size)
        val er = metrics.topErrors[0]
        assertEquals("NotRegistered", er.errorCode)
        assertEquals(1, er.count)

        verify(mongo, times(3)).aggregate(any<Aggregation>(), eq(PushDeliveryAttempt::class.java), eq(Document::class.java))
    }
}
