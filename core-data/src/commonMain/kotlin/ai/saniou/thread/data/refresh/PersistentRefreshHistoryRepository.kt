package ai.saniou.thread.data.refresh

import ai.saniou.thread.domain.refresh.DiagnosticSanitizer
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshHistory
import ai.saniou.thread.domain.refresh.RefreshHistoryRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PersistentRefreshHistoryRepository(
    private val settings: SettingsRepository,
) : RefreshHistoryRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val state = MutableStateFlow<Map<String, RefreshHistory>>(emptyMap())
    private var loaded = false

    override fun observe(): Flow<Map<String, RefreshHistory>> = flow {
        ensureLoaded()
        emitAll(state)
    }

    override suspend fun recordSuccess(key: String, label: String, finishedAtEpochMillis: Long) {
        mutate { current ->
            current + (key to (current[key] ?: RefreshHistory(key, label, finishedAtEpochMillis)).copy(
                label = label,
                lastAttemptAtEpochMillis = finishedAtEpochMillis,
                lastSuccessAtEpochMillis = finishedAtEpochMillis,
                consecutiveFailureCount = 0,
                failureKind = null,
                message = null,
                rateLimitUntilEpochMillis = null,
            ))
        }
    }

    override suspend fun recordFailure(
        key: String,
        label: String,
        finishedAtEpochMillis: Long,
        kind: RefreshFailureKind,
        message: String,
    ) {
        mutate { current ->
            val previous = current[key]
            val failures = (previous?.consecutiveFailureCount ?: 0) + 1
            val rateLimitUntil = if (kind == RefreshFailureKind.RATE_LIMIT) {
                finishedAtEpochMillis + (RATE_LIMIT_BASE_MILLIS * failures).coerceAtMost(RATE_LIMIT_MAX_MILLIS)
            } else {
                null
            }
            current + (key to RefreshHistory(
                key = key,
                label = label,
                lastAttemptAtEpochMillis = finishedAtEpochMillis,
                lastSuccessAtEpochMillis = previous?.lastSuccessAtEpochMillis,
                lastFailureAtEpochMillis = finishedAtEpochMillis,
                consecutiveFailureCount = failures,
                failureKind = kind,
                message = DiagnosticSanitizer.sanitize(message),
                rateLimitUntilEpochMillis = rateLimitUntil,
            ))
        }
    }

    override suspend fun clearSource(sourceId: String) {
        mutate { current ->
            current.filterKeys { key ->
                key != "reader:$sourceId" && !key.startsWith("forum:$sourceId:")
            }
        }
    }

    private suspend fun ensureLoaded() = mutex.withLock {
        if (!loaded) loadLocked()
    }

    private suspend fun mutate(transform: (Map<String, RefreshHistory>) -> Map<String, RefreshHistory>) {
        mutex.withLock {
            if (!loaded) loadLocked()
            val updated = transform(state.value)
                .entries
                .sortedByDescending { it.value.lastAttemptAtEpochMillis }
                .take(MAX_HISTORY_ENTRIES)
                .associate { it.toPair() }
            state.value = updated
            settings.saveValue(STORAGE_KEY, json.encodeToString(updated.values.map(PersistedHistory::fromDomain)))
        }
    }

    private suspend fun loadLocked() {
        val raw = settings.getValue<String>(STORAGE_KEY)
        state.value = runCatching {
            json.decodeFromString<List<PersistedHistory>>(raw.orEmpty())
                .associate { it.key to it.toDomain() }
        }.getOrDefault(emptyMap())
        loaded = true
    }

    private companion object {
        const val STORAGE_KEY = "refresh_history_v1"
        const val MAX_HISTORY_ENTRIES = 200
        const val RATE_LIMIT_BASE_MILLIS = 60_000L
        const val RATE_LIMIT_MAX_MILLIS = 15 * 60_000L
    }
}

@Serializable
private data class PersistedHistory(
    val key: String,
    val label: String,
    val lastAttemptAtEpochMillis: Long,
    val lastSuccessAtEpochMillis: Long? = null,
    val lastFailureAtEpochMillis: Long? = null,
    val consecutiveFailureCount: Int = 0,
    val failureKind: String? = null,
    val message: String? = null,
    val rateLimitUntilEpochMillis: Long? = null,
) {
    fun toDomain() = RefreshHistory(
        key = key,
        label = label,
        lastAttemptAtEpochMillis = lastAttemptAtEpochMillis,
        lastSuccessAtEpochMillis = lastSuccessAtEpochMillis,
        lastFailureAtEpochMillis = lastFailureAtEpochMillis,
        consecutiveFailureCount = consecutiveFailureCount.coerceAtLeast(0),
        failureKind = failureKind?.let { value -> RefreshFailureKind.entries.firstOrNull { it.name == value } },
        message = DiagnosticSanitizer.sanitize(message),
        rateLimitUntilEpochMillis = rateLimitUntilEpochMillis,
    )

    companion object {
        fun fromDomain(value: RefreshHistory) = PersistedHistory(
            key = value.key,
            label = value.label,
            lastAttemptAtEpochMillis = value.lastAttemptAtEpochMillis,
            lastSuccessAtEpochMillis = value.lastSuccessAtEpochMillis,
            lastFailureAtEpochMillis = value.lastFailureAtEpochMillis,
            consecutiveFailureCount = value.consecutiveFailureCount,
            failureKind = value.failureKind?.name,
            message = DiagnosticSanitizer.sanitize(value.message),
            rateLimitUntilEpochMillis = value.rateLimitUntilEpochMillis,
        )
    }
}
