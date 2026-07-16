package ai.saniou.thread.data.reader

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.ReaderRefreshReport
import ai.saniou.thread.domain.model.reader.ReaderSchedulerState
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.repository.ReaderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.coroutineContext
import kotlin.time.Clock

class DefaultReaderRefreshScheduler(
    private val repository: ReaderRepository,
) : ReaderRefreshScheduler {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow(ReaderSchedulerState())
    override val state: StateFlow<ReaderSchedulerState> = _state.asStateFlow()

    private var sources: List<FeedSource> = emptyList()
    private var observerJob: Job? = null
    private var loopJob: Job? = null

    override fun start() {
        if (loopJob?.isActive == true) return
        _state.update { it.copy(isRunning = true) }
        observerJob = scope.launch {
            repository.getAllFeedSources().collect { latest ->
                sources = latest
                val dueCount = dueSources(latest).size
                _state.update { it.copy(dueCount = dueCount) }
                if (dueCount > 0) launch { refreshDueNow() }
            }
        }
        loopJob = scope.launch {
            while (coroutineContext.isActive) {
                refreshDueNow()
                delay(nextCheckDelayMillis(sources))
            }
        }
    }

    override fun stop() {
        observerJob?.cancel()
        loopJob?.cancel()
        observerJob = null
        loopJob = null
        _state.update { it.copy(isRunning = false, refreshingSourceIds = emptySet()) }
    }

    override suspend fun refreshDueNow(): ReaderRefreshReport = refreshMutex.withLock {
        val due = dueSources(sources)
        _state.update { it.copy(dueCount = due.size, refreshingSourceIds = due.mapTo(mutableSetOf()) { source -> source.id }) }
        val results = supervisorScope {
            due.map { source -> async { source.id to repository.refreshFeed(source.id, forceRefresh = false) } }
                .awaitAll()
        }
        val now = Clock.System.now().toEpochMilliseconds()
        _state.update {
            it.copy(
                dueCount = dueSources(sources, now).size,
                refreshingSourceIds = emptySet(),
                lastRunEpochMillis = now,
            )
        }
        ReaderRefreshReport(
            refreshedSourceIds = results.mapNotNullTo(mutableSetOf()) { (id, result) -> id.takeIf { result.isSuccess } },
            failures = results.mapNotNull { (id, result) ->
                result.exceptionOrNull()?.let { error -> id to (error.message ?: "Unknown error") }
            }.toMap(),
        )
    }

    internal fun dueSources(
        candidates: List<FeedSource>,
        nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): List<FeedSource> = candidates.filter { source ->
        source.autoRefresh &&
            (source.lastUpdate <= 0L || nowEpochMillis - source.lastUpdate >= source.refreshInterval)
    }

    private fun nextCheckDelayMillis(candidates: List<FeedSource>): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val nextDueIn = candidates.asSequence()
            .filter { it.autoRefresh && it.lastUpdate > 0L }
            .map { (it.lastUpdate + it.refreshInterval - now).coerceAtLeast(MIN_CHECK_DELAY) }
            .minOrNull()
        return (nextDueIn ?: MAX_CHECK_DELAY).coerceIn(MIN_CHECK_DELAY, MAX_CHECK_DELAY)
    }

    private companion object {
        const val MIN_CHECK_DELAY = 10_000L
        const val MAX_CHECK_DELAY = 300_000L
    }
}
