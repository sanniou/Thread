package ai.saniou.feature.feed.workflow

import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.usecase.feed.GetTimelineUseCase
import ai.saniou.thread.domain.usecase.feed.RefreshTimelineUseCase
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.usecase.refresh.ObserveRefreshDiagnosticsUseCase
import ai.saniou.thread.domain.model.workspace.ListAnchor
import ai.saniou.thread.domain.usecase.workspace.ObserveWorkspaceSessionUseCase
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val getTimeline: GetTimelineUseCase,
    private val refreshTimeline: RefreshTimelineUseCase,
    getAvailableSources: GetAvailableSourcesUseCase,
    observeRefreshDiagnostics: ObserveRefreshDiagnosticsUseCase,
    observeWorkspaceSession: ObserveWorkspaceSessionUseCase,
    private val updateWorkspaceSession: UpdateWorkspaceSessionUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(FeedContract.State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<FeedContract.Effect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    val timeline: Flow<PagingData<TimelineItem>> = state
        .map { it.selectedSourceIds to it.includeReader }
        .distinctUntilChanged()
        .flatMapLatest { (sourceIds, includeReader) ->
            getTimeline(sourceIds = sourceIds, includeReader = includeReader)
        }
        .cachedIn(screenModelScope)

    init {
        screenModelScope.launch {
            kotlinx.coroutines.flow.combine(
                getAvailableSources(),
                observeWorkspaceSession(),
            ) { allSources, session -> allSources to session.feed }
                .collect { (allSources, restored) ->
                val sources = allSources
                    .filter { it.capabilities.supportsFeedAggregation }
                    .sortedBy { it.name }
                _state.update { current ->
                    val availableIds = sources.mapTo(mutableSetOf()) { it.id }
                    val selected = if (restored.hasExplicitSourceSelection) {
                        restored.selectedSourceIds.intersect(availableIds)
                    } else if (current.sources.isEmpty()) {
                        availableIds
                    } else {
                        current.selectedSourceIds.intersect(availableIds)
                    }
                    current.copy(
                        sources = sources.map { FeedContract.SourceOption(it.id, it.name) },
                        selectedSourceIds = selected,
                        includeReader = restored.includeReader,
                        listAnchor = restored.listAnchor,
                    )
                }
            }
        }
        screenModelScope.launch {
            observeRefreshDiagnostics().collect { tasks ->
                _state.update { current ->
                    current.copy(
                        refreshFailures = tasks.values
                            .filter { it.status == RefreshStatus.FAILED }
                            .filter { it.key.startsWith("forum:") || it.key.startsWith("reader:") }
                            .sortedByDescending { it.finishedAtEpochMillis },
                    )
                }
            }
        }
    }

    fun onEvent(event: FeedContract.Event) {
        when (event) {
            is FeedContract.Event.ToggleSource -> toggleSource(event.sourceId)
            FeedContract.Event.ToggleReader -> mutateSelection { it.copy(includeReader = !it.includeReader) }
            FeedContract.Event.SelectAllSources -> mutateSelection { state ->
                state.copy(selectedSourceIds = state.sources.mapTo(mutableSetOf()) { it.id })
            }
            FeedContract.Event.ClearForumSources -> mutateSelection { it.copy(selectedSourceIds = emptySet()) }
            FeedContract.Event.Refresh -> refresh()
            is FeedContract.Event.ListPositionChanged -> {
                _state.update {
                    it.copy(listAnchor = ListAnchor(event.contextKey, event.index, event.offset))
                }
                persistFeedState()
            }
            FeedContract.Event.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun toggleSource(sourceId: String) {
        _state.update { state ->
            val next = state.selectedSourceIds.toMutableSet().apply {
                if (!add(sourceId)) remove(sourceId)
            }
            state.copy(selectedSourceIds = next)
        }
        persistFeedState()
    }

    private fun mutateSelection(transform: (FeedContract.State) -> FeedContract.State) {
        _state.update(transform)
        persistFeedState()
    }

    private fun persistFeedState() {
        val current = _state.value
        screenModelScope.launch {
            updateWorkspaceSession { session ->
                session.copy(
                    feed = session.feed.copy(
                        selectedSourceIds = current.selectedSourceIds,
                        hasExplicitSourceSelection = true,
                        includeReader = current.includeReader,
                        listAnchor = current.listAnchor,
                    ),
                    updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    private fun refresh() {
        if (_state.value.isRefreshing) return
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true, message = null) }
            val current = _state.value
            val report = refreshTimeline(
                sourceIds = current.selectedSourceIds,
                includeReader = current.includeReader,
            )
            val failureCount = report.sourceFailures.size + report.readerFailures.size
            val successCount = report.refreshedSourceIds.size + report.refreshedReaderSourceIds.size
            val message = when {
                report.isSuccess -> "已刷新 $successCount 个来源"
                report.hasAnySuccess -> "已刷新 $successCount 个来源，$failureCount 个来源失败"
                else -> "刷新失败：$failureCount 个来源不可用"
            }
            _state.update { it.copy(isRefreshing = false, message = message) }
            _effect.emit(FeedContract.Effect.RefreshPaging)
        }
    }
}
