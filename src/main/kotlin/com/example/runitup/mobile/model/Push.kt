package  com.example.runitup.mobile.model
import java.time.Instant

data class TimeRange(
    val from: Instant,
    val to: Instant
)

data class OverviewMetrics(
    val attempts: Long,
    val success: Long,
    val failed: Long,
    val successRate: Double,
    val invalidTokenCount: Long
)

data class BreakdownRow(
    val key: String,
    val attempts: Long,
    val success: Long,
    val failed: Long,
    val successRate: Double
)

data class ErrorRow(
    val errorCode: String,
    val count: Long
)

data class SessionMetrics(
    val sessionId: String,
    val overview: OverviewMetrics,
    val byTemplate: List<BreakdownRow>,
    val topErrors: List<ErrorRow>
)

data class UserMetrics(
    val userId: String,
    val overview: OverviewMetrics,
    val byTemplate: List<BreakdownRow>,
    val topErrors: List<ErrorRow>
)
