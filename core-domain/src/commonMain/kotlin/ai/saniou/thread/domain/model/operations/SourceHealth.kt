package ai.saniou.thread.domain.model.operations

import ai.saniou.thread.domain.refresh.RefreshFailureKind

enum class ContentSourceKind { FORUM, READER }

enum class SourceOperationalState {
    READY,
    DISABLED,
    REFRESHING,
    OFFLINE,
    AUTHENTICATION_REQUIRED,
    RATE_LIMITED,
    DEGRADED,
}

data class SourceHealth(
    val id: String,
    val name: String,
    val kind: ContentSourceKind,
    val state: SourceOperationalState,
    val enabled: Boolean,
    val primaryItemCount: Long,
    val secondaryItemCount: Long = 0,
    val lastContentAtEpochMillis: Long? = null,
    val lastRefreshAtEpochMillis: Long? = null,
    val lastSuccessfulRefreshAtEpochMillis: Long? = null,
    val cacheAgeMillis: Long? = null,
    val refreshAttempt: Int = 0,
    val consecutiveFailureCount: Int = 0,
    val rateLimitUntilEpochMillis: Long? = null,
    val failureKind: RefreshFailureKind? = null,
    val message: String? = null,
    val capabilities: Set<String> = emptySet(),
)

data class OperationsSnapshot(
    val sources: List<SourceHealth> = emptyList(),
    val activeRefreshCount: Int = 0,
    val failedRefreshCount: Int = 0,
    val cachedItemCount: Long = 0,
    val storageDirectory: String = "",
)

data class DiagnosticExport(
    val payload: String,
    val generatedAtEpochMillis: Long,
    val sourceCount: Int,
    val redacted: Boolean = true,
)
