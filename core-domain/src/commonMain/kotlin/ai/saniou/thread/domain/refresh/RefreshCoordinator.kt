package ai.saniou.thread.domain.refresh

import kotlinx.coroutines.flow.StateFlow

enum class RefreshStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
}

enum class RefreshFailureKind {
    OFFLINE,
    TIMEOUT,
    RATE_LIMIT,
    AUTHENTICATION,
    REMOTE,
    UNKNOWN,
}

data class RefreshPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 350,
    val maxDelayMillis: Long = 2_000,
) {
    init {
        require(maxAttempts > 0)
        require(initialDelayMillis >= 0)
        require(maxDelayMillis >= initialDelayMillis)
    }
}

data class RefreshTaskState(
    val key: String,
    val label: String,
    val status: RefreshStatus,
    val attempt: Int,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long? = null,
    val failureKind: RefreshFailureKind? = null,
    val message: String? = null,
)

/** Shared boundary for refresh serialization, retry policy, and user-visible diagnostics. */
interface RefreshCoordinator {
    val states: StateFlow<Map<String, RefreshTaskState>>

    suspend fun <T> execute(
        key: String,
        label: String = key,
        policy: RefreshPolicy = RefreshPolicy(),
        operation: suspend () -> Result<T>,
    ): Result<T>

    fun clear(key: String)
}
