package ai.saniou.thread.data.refresh

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshPolicy
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.refresh.RefreshTaskState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class DefaultRefreshCoordinator : RefreshCoordinator {
    private val mutableStates = MutableStateFlow<Map<String, RefreshTaskState>>(emptyMap())
    override val states: StateFlow<Map<String, RefreshTaskState>> = mutableStates.asStateFlow()

    private val mutexGuard = Mutex()
    private val taskMutexes = mutableMapOf<String, Mutex>()

    override suspend fun <T> execute(
        key: String,
        label: String,
        policy: RefreshPolicy,
        operation: suspend () -> Result<T>,
    ): Result<T> {
        require(key.isNotBlank()) { "Refresh key must not be blank" }
        val taskMutex = mutexGuard.withLock { taskMutexes.getOrPut(key, ::Mutex) }
        taskMutex.lock()
        try {
            val startedAt = now()
            var delayMillis = policy.initialDelayMillis
            var lastError: Throwable? = null
            for (attempt in 1..policy.maxAttempts) {
                publish(
                    RefreshTaskState(
                        key = key,
                        label = label,
                        status = RefreshStatus.RUNNING,
                        attempt = attempt,
                        startedAtEpochMillis = startedAt,
                    )
                )
                val result = try {
                    withContext(ioDispatcher) { operation() }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    Result.failure(error)
                }
                if (result.isSuccess) {
                    publish(
                        RefreshTaskState(
                            key = key,
                            label = label,
                            status = RefreshStatus.SUCCEEDED,
                            attempt = attempt,
                            startedAtEpochMillis = startedAt,
                            finishedAtEpochMillis = now(),
                        )
                    )
                    return result
                }

                val error = result.exceptionOrNull() ?: IllegalStateException("Unknown refresh error")
                lastError = error
                val kind = RefreshFailureClassifier.classify(error)
                if (attempt == policy.maxAttempts || !kind.isRetryable) {
                    publishFailure(key, label, attempt, startedAt, kind, error)
                    return Result.failure(error)
                }
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(policy.maxDelayMillis)
            }
            return Result.failure(lastError ?: IllegalStateException("Refresh did not run"))
        } finally {
            taskMutex.unlock()
        }
    }

    override fun clear(key: String) {
        mutableStates.update { it - key }
    }

    private fun publish(state: RefreshTaskState) {
        mutableStates.update { it + (state.key to state) }
    }

    private fun publishFailure(
        key: String,
        label: String,
        attempt: Int,
        startedAt: Long,
        kind: RefreshFailureKind,
        error: Throwable,
    ) {
        publish(
            RefreshTaskState(
                key = key,
                label = label,
                status = RefreshStatus.FAILED,
                attempt = attempt,
                startedAtEpochMillis = startedAt,
                finishedAtEpochMillis = now(),
                failureKind = kind,
                message = error.message ?: error::class.simpleName ?: "Unknown error",
            )
        )
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}

internal object RefreshFailureClassifier {
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

private val RefreshFailureKind.isRetryable: Boolean
    get() = this == RefreshFailureKind.OFFLINE ||
        this == RefreshFailureKind.TIMEOUT ||
        this == RefreshFailureKind.RATE_LIMIT ||
        this == RefreshFailureKind.REMOTE
