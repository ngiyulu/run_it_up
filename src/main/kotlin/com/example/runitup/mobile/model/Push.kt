package  com.example.runitup.mobile.model
import java.time.Instant

data class TimeRange(
    val from: Instant,
    val to: Instant
)

data class OverviewMetrics(
    val attempts: Int,
    val success: Int,
    val failed: Int,
    val successRate: Double,
    val invalidTokenCount: Int
)

data class BreakdownRow(
    val key: String,
    val attempts: Int,
    val success: Int,
    val failed: Int,
    val successRate: Double
)

data class ErrorRow(
    val errorCode: String,
    val count: Int
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
