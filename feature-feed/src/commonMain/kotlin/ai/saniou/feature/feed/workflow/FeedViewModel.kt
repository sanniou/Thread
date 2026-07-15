package ai.saniou.feature.feed.workflow

import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.usecase.feed.GetTimelineUseCase
import ai.saniou.thread.domain.usecase.feed.RefreshTimelineUseCase
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
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
) : ScreenModel {

    private val availableSources = getAvailableSources()
        .filter { it.capabilities.supportsFeedAggregation }
        .sortedBy { it.name }

    private val _state = MutableStateFlow(
        FeedContract.State(
            sources = availableSources.map { FeedContract.SourceOption(it.id, it.name) },
            selectedSourceIds = availableSources.mapTo(mutableSetOf()) { it.id },
        )
    )
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

    fun onEvent(event: FeedContract.Event) {
        when (event) {
            is FeedContract.Event.ToggleSource -> toggleSource(event.sourceId)
            FeedContract.Event.ToggleReader -> _state.update { it.copy(includeReader = !it.includeReader) }
            FeedContract.Event.SelectAllSources -> _state.update { state ->
                state.copy(selectedSourceIds = state.sources.mapTo(mutableSetOf()) { it.id })
            }
            FeedContract.Event.ClearForumSources -> _state.update { it.copy(selectedSourceIds = emptySet()) }
            FeedContract.Event.Refresh -> refresh()
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
