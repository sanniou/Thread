package ai.saniou.thread.domain.refresh

import kotlinx.coroutines.flow.Flow

/** Durable refresh outcome retained after process death and volatile task cleanup. */
data class RefreshHistory(
    val key: String,
    val label: String,
    val lastAttemptAtEpochMillis: Long,
    val lastSuccessAtEpochMillis: Long? = null,
    val lastFailureAtEpochMillis: Long? = null,
    val consecutiveFailureCount: Int = 0,
    val failureKind: RefreshFailureKind? = null,
    val message: String? = null,
    val rateLimitUntilEpochMillis: Long? = null,
)

interface RefreshHistoryRepository {
    fun observe(): Flow<Map<String, RefreshHistory>>

    suspend fun recordSuccess(key: String, label: String, finishedAtEpochMillis: Long)

    suspend fun recordFailure(
        key: String,
        label: String,
        finishedAtEpochMillis: Long,
        kind: RefreshFailureKind,
        message: String,
    )

    suspend fun clearSource(sourceId: String)
}

/** Redacts credential-shaped fragments before they can reach durable history or UI. */
object DiagnosticSanitizer {
    private val bearer = Regex("(?i)bearer\\s+[a-z0-9._~+/=-]+")
    private val secretField = Regex(
        "(?i)(authorization|cookie|set-cookie|password|passwd|token|api[_-]?key|secret)\\s*[:=]\\s*([^\\s,;]+)"
    )
    private val queryValue = Regex("([?&])([^=\\s]+)=([^&\\s]+)")

    fun sanitize(message: String?): String? = message
        ?.replace(bearer, "Bearer [REDACTED]")
        ?.replace(secretField) { "${it.groupValues[1]}=[REDACTED]" }
        ?.replace(queryValue) { "${it.groupValues[1]}${it.groupValues[2]}=[REDACTED]" }
        ?.replace(Regex("[\\r\\n\\t]+"), " ")
        ?.trim()
        ?.take(500)
        ?.takeIf(String::isNotBlank)
}
