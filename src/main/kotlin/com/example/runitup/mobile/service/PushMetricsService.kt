package com.example.runitup.mobile.service

// package com.example.runitup.mobile.service.metrics


import com.example.runitup.mobile.model.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import kotlin.math.round

@Service
class PushMetricsService(
    private val mongo: MongoTemplate
) {
    private val COLL = "pushDeliveryAttempt" // Spring will map class -> collection; override if needed

    private fun matchBase(range: TimeRange, sessionId: String? = null, userId: String? = null): MatchOperation {
        val c = Criteria()
            .andOperator(
                Criteria.where("requestedAt").gte(range.from),
                Criteria.where("requestedAt").lt(range.to)
            )

        val ands = mutableListOf<Criteria>(c)
        // Scope by session or user if provided (via eventId lookup or direct fields).
        sessionId?.let {
            // We stored eventId on attempts; event -> triggerRefId (sessionId).
            // Fastest path: also persist sessionId on attempts. If not, do a $lookup to event collection.
            // Below assumes attempts also have eventId only; we $lookup to events to filter by triggerRefId.
        }
        userId?.let {
            ands += Criteria.where("userId").`is`(userId)
        }
        return Aggregation.match(Criteria().andOperator(*ands.toTypedArray()))
    }

    private fun rate(success: Int, attempts: Int): Double =
        if (attempts == 0) 0.0 else round((success.toDouble() / attempts.toDouble()) * 1000.0) / 10.0

    fun overview(range: TimeRange): OverviewMetrics {
        val statusSucceeded = ConditionalOperators.`when`(Criteria.where("status").`is`("SUCCESS")).then(1).otherwise(0)
        val statusFailed    = ConditionalOperators.`when`(Criteria.where("status").`is`("FAILED")).then(1).otherwise(0)
        val invalidToken    = ConditionalOperators.`when`(Criteria.where("errorCode").`is`("InvalidToken")).then(1).otherwise(0)

        val agg = Aggregation.newAggregation(
            matchBase(range),
            Aggregation.group()
                .sum(statusSucceeded).`as`("success")
                .sum(statusFailed).`as`("failed")
                .sum(invalidToken).`as`("invalidTokens")
                .count().`as`("attempts")
        )

        val result = mongo.aggregate(agg, PushDeliveryAttempt::class.java, org.bson.Document::class.java)
            .mappedResults.firstOrNull()

        val attempts = result?.getInteger("attempts") ?: 0
        val success = result?.getInteger("success") ?: 0
        val failed = result?.getInteger("failed") ?: 0
        val invalid = result?.getInteger("invalidTokens") ?: 0

        return OverviewMetrics(
            attempts = attempts,
            success = success,
            failed = failed,
            successRate = rate(success, attempts),
            invalidTokenCount = invalid
        )
    }

    fun breakdownByField(range: TimeRange, field: String): List<BreakdownRow> {
        val successCond = ConditionalOperators.`when`(Criteria.where("status").`is`("SUCCESS")).then(1).otherwise(0)
        val failedCond  = ConditionalOperators.`when`(Criteria.where("status").`is`("FAILED")).then(1).otherwise(0)

        val agg = Aggregation.newAggregation(
            matchBase(range),
            Aggregation.group(field)
                .sum(successCond).`as`("success")
                .sum(failedCond).`as`("failed")
                .count().`as`("attempts"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "attempts"))
        )

        val docs = mongo.aggregate(agg, PushDeliveryAttempt::class.java, org.bson.Document::class.java).mappedResults
        return docs.map {
            val key = it.getString("_id") ?: "UNKNOWN"
            val attempts = it.getInteger("attempts") ?: 0
            val success = it.getInteger("success") ?: 0
            val failed = it.getInteger("failed") ?: 0
            BreakdownRow(
                key = key,
                attempts = attempts,
                success = success,
                failed = failed,
                successRate = rate(success, attempts)
            )
        }
    }

    fun topErrors(range: TimeRange, limit: Int = 20): List<ErrorRow> {
        val agg = Aggregation.newAggregation(
            matchBase(range),
            Aggregation.match(Criteria.where("errorCode").ne(null)),
            Aggregation.group("errorCode").count().`as`("count"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "count")),
            Aggregation.limit(limit.toLong())
        )
        val docs = mongo.aggregate(agg, PushDeliveryAttempt::class.java, org.bson.Document::class.java).mappedResults
        return docs.map { ErrorRow(it.getString("_id") ?: "UNKNOWN", it.getInteger("count") ?: 0) }
    }

    // Convenience wrappers
    fun byTemplate(range: TimeRange): List<BreakdownRow> = breakdownByField(range, "templateId")
    fun byVendor(range: TimeRange): List<BreakdownRow>    = breakdownByField(range, "vendor")
    fun byPhoneType(range: TimeRange): List<BreakdownRow> = breakdownByField(range, "phoneType")

    /**
     * If you want to scope metrics to a sessionId but attempts don't store sessionId,
     * create attempts with `sessionId` field. Otherwise you'd need a $lookup to the events collection.
     * The code below assumes you **added `sessionId` and `templateId` fields** to attempts.
     */
    fun sessionMetrics(sessionId: String, range: TimeRange): SessionMetrics {
        val scoped = TimeRange(range.from, range.to)
        val match = Aggregation.match(
            Criteria.where("requestedAt").gte(range.from).lt(range.to)
                .and("sessionId").`is`(sessionId)
        )
        val ov = overviewWithExtraMatch(scoped, match)
        val byTemplate = breakdownWithExtraMatch(scoped, match, "templateId")
        val errors = topErrorsWithExtraMatch(scoped, match, 20)
        return SessionMetrics(sessionId, ov, byTemplate, errors)
    }

    fun userMetrics(userId: String, range: TimeRange): UserMetrics {
        val match = Aggregation.match(
            Criteria.where("requestedAt").gte(range.from).lt(range.to)
                .and("userId").`is`(userId)
        )
        val ov = overviewWithExtraMatch(range, match)
        val byTemplate = breakdownWithExtraMatch(range, match, "templateId")
        val errors = topErrorsWithExtraMatch(range, match, 20)
        return UserMetrics(userId, ov, byTemplate, errors)
    }

    // ---- helpers with extra match ----

    private fun overviewWithExtraMatch(range: TimeRange, extra: MatchOperation): OverviewMetrics {
        val statusSucceeded = ConditionalOperators.`when`(Criteria.where("status").`is`("SUCCESS")).then(1).otherwise(0)
        val statusFailed    = ConditionalOperators.`when`(Criteria.where("status").`is`("FAILED")).then(1).otherwise(0)
        val invalidToken    = ConditionalOperators.`when`(Criteria.where("errorCode").`is`("InvalidToken")).then(1).otherwise(0)

        val agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("requestedAt").gte(range.from).lt(range.to)),
            extra,
            Aggregation.group()
                .sum(statusSucceeded).`as`("success")
                .sum(statusFailed).`as`("failed")
                .sum(invalidToken).`as`("invalidTokens")
                .count().`as`("attempts")
        )

        val result = mongo.aggregate(agg, PushDeliveryAttempt::class.java, org.bson.Document::class.java)
            .mappedResults.firstOrNull()

        val attempts = result?.getInteger("attempts") ?: 0
        val success = result?.getInteger("success") ?: 0
        val failed = result?.getInteger("failed") ?: 0
        val invalid = result?.getInteger("invalidTokens") ?: 0

        return OverviewMetrics(
            attempts = attempts,
            success = success,
            failed = failed,
            successRate = rate(success, attempts),
            invalidTokenCount = invalid
        )
    }

    private fun breakdownWithExtraMatch(range: TimeRange, extra: MatchOperation, field: String): List<BreakdownRow> {
        val successCond = ConditionalOperators.`when`(Criteria.where("status").`is`("SUCCESS")).then(1).otherwise(0)
        val failedCond  = ConditionalOperators.`when`(Criteria.where("status").`is`("FAILED")).then(1).otherwise(0)

        val agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("requestedAt").gte(range.from).lt(range.to)),
            extra,
            Aggregation.group(field)
                .sum(successCond).`as`("success")
                .sum(failedCond).`as`("failed")
                .count().`as`("attempts"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "attempts"))
        )

        val docs = mongo.aggregate(agg, PushDeliveryAttempt::class.java, org.bson.Document::class.java).mappedResults
        return docs.map {
            val key = it.getString("_id") ?: "UNKNOWN"
            val attempts = it.getInteger("attempts") ?: 0
            val success = it.getInteger("success") ?: 0
            val failed = it.getInteger("failed") ?: 0
            BreakdownRow(
                key = key,
                attempts = attempts,
                success = success,
                failed = failed,
                successRate = rate(success, attempts)
            )
        }
    }

    private fun topErrorsWithExtraMatch(range: TimeRange, extra: MatchOperation, limit: Int): List<ErrorRow> {
        val agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("requestedAt").gte(range.from).lt(range.to)),
            extra,
            Aggregation.match(Criteria.where("errorCode").ne(null)),
            Aggregation.group("errorCode").count().`as`("count"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "count")),
            Aggregation.limit(limit.toLong())
        )
        val docs = mongo.aggregate(agg, PushDeliveryAttempt::class.java, org.bson.Document::class.java).mappedResults
        return docs.map { ErrorRow(it.getString("_id") ?: "UNKNOWN", it.getInteger("count") ?: 0) }
    }
}
