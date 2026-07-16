package ai.saniou.thread.data.refresh

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshPolicy
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.refresh.RefreshTaskState
import ai.saniou.thread.domain.refresh.FailureClassifier
import ai.saniou.thread.domain.refresh.DiagnosticSanitizer
import ai.saniou.thread.domain.refresh.RefreshHistoryRepository
import ai.saniou.thread.domain.repository.IdentityRepository
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

class DefaultRefreshCoordinator(
    private val historyRepository: RefreshHistoryRepository? = null,
    private val identityRepository: IdentityRepository? = null,
) : RefreshCoordinator {
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
                    val finishedAt = now()
                    publish(
                        RefreshTaskState(
                            key = key,
                            label = label,
                            status = RefreshStatus.SUCCEEDED,
                            attempt = attempt,
                            startedAtEpochMillis = startedAt,
                            finishedAtEpochMillis = finishedAt,
                        )
                    )
                    historyRepository?.recordSuccess(key, label, finishedAt)
                    key.forumSourceId()?.let { identityRepository?.markAuthenticated(it, finishedAt) }
                    return result
                }

                val error = result.exceptionOrNull() ?: IllegalStateException("Unknown refresh error")
                lastError = error
                val kind = FailureClassifier.classify(error)
                if (attempt == policy.maxAttempts || !kind.isRetryable) {
                    val finishedAt = now()
                    publishFailure(key, label, attempt, startedAt, finishedAt, kind, error)
                    historyRepository?.recordFailure(
                        key = key,
                        label = label,
                        finishedAtEpochMillis = finishedAt,
                        kind = kind,
                        message = error.message ?: error::class.simpleName ?: "Unknown error",
                    )
                    if (kind == RefreshFailureKind.AUTHENTICATION) {
                        key.forumSourceId()?.let { sourceId ->
                            identityRepository?.markExpired(sourceId, error.message, finishedAt)
                        }
                    }
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
        finishedAt: Long,
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
                finishedAtEpochMillis = finishedAt,
                failureKind = kind,
                message = DiagnosticSanitizer.sanitize(error.message ?: error::class.simpleName) ?: "Unknown error",
            )
        )
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}

private fun String.forumSourceId(): String? =
    takeIf { it.startsWith("forum:") }
        ?.removePrefix("forum:")
        ?.substringBefore(':')
        ?.takeIf(String::isNotBlank)

private val RefreshFailureKind.isRetryable: Boolean
    get() = this == RefreshFailureKind.OFFLINE ||
        this == RefreshFailureKind.TIMEOUT ||
        this == RefreshFailureKind.RATE_LIMIT ||
        this == RefreshFailureKind.REMOTE
