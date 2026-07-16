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

/** One common error vocabulary for retry policy, diagnostics and UI messaging. */
object FailureClassifier {
    fun classify(error: Throwable): RefreshFailureKind {
        val description = generateSequence(error) { it.cause }
            .joinToString(" ") { "${it::class.simpleName.orEmpty()} ${it.message.orEmpty()}" }
            .lowercase()
        return when {
            "timeout" in description || "timed out" in description -> RefreshFailureKind.TIMEOUT
            "429" in description || "too many requests" in description -> RefreshFailureKind.RATE_LIMIT
            "401" in description || "403" in description || "unauthorized" in description ||
                "forbidden" in description || "login" in description || "cookie" in description ->
                RefreshFailureKind.AUTHENTICATION
            "unknownhost" in description || "unresolved" in description ||
                "network is unreachable" in description || "connectexception" in description ||
                "connection refused" in description || "offline" in description -> RefreshFailureKind.OFFLINE
            "500" in description || "502" in description || "503" in description ||
                "504" in description || "server error" in description -> RefreshFailureKind.REMOTE
            else -> RefreshFailureKind.UNKNOWN
        }
    }
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
