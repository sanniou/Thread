package ai.saniou.feature.feed.workflow

import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.usecase.feed.GetTimelineUseCase
import ai.saniou.thread.domain.usecase.feed.RefreshTimelineUseCase
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
import ai.saniou.thread.domain.usecase.social.ObserveSocialSourcesUseCase
import ai.saniou.thread.domain.usecase.social.InteractWithSocialPostUseCase
import ai.saniou.thread.domain.usecase.social.LoadOlderSocialTimelineUseCase
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
import org.jetbrains.compose.resources.getString
import thread.feature_feed.generated.resources.Res
import thread.feature_feed.generated.resources.s_4d8b4193a3
import thread.feature_feed.generated.resources.s_52e6d8f7c0
import thread.feature_feed.generated.resources.s_6414520846
import thread.feature_feed.generated.resources.s_9fbd2eca51
import thread.feature_feed.generated.resources.s_b93442c95f
import thread.feature_feed.generated.resources.s_f15797df4e

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val getTimeline: GetTimelineUseCase,
    private val refreshTimeline: RefreshTimelineUseCase,
    getAvailableSources: GetAvailableSourcesUseCase,
    observeSocialSources: ObserveSocialSourcesUseCase,
    private val interactWithSocialPost: InteractWithSocialPostUseCase,
    private val loadOlderSocialTimeline: LoadOlderSocialTimelineUseCase,
    observeRefreshDiagnostics: ObserveRefreshDiagnosticsUseCase,
    observeWorkspaceSession: ObserveWorkspaceSessionUseCase,
    private val updateWorkspaceSession: UpdateWorkspaceSessionUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(FeedContract.State())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<FeedContract.Effect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    val timeline: Flow<PagingData<TimelineItem>> = state
        .map {
            TimelineSelection(
                forumSourceIds = it.selectedSourceIds,
                includeReader = it.includeReader,
                socialSourceIds = it.selectedSocialSourceIds,
                includeSocial = it.includeSocial,
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { selection ->
            getTimeline(
                sourceIds = selection.forumSourceIds,
                includeReader = selection.includeReader,
                socialSourceIds = selection.socialSourceIds,
                includeSocial = selection.includeSocial,
            )
        }
        .cachedIn(screenModelScope)

    init {
        screenModelScope.launch {
            kotlinx.coroutines.flow.combine(
                getAvailableSources(),
                observeSocialSources(),
                observeWorkspaceSession(),
            ) { allSources, socialSources, session -> Triple(allSources, socialSources, session.feed) }
                .collect { (allSources, allSocialSources, restored) ->
                val sources = allSources
                    .filter { it.capabilities.supportsFeedAggregation }
                    .sortedBy { it.name }
                _state.update { current ->
                    val availableIds = sources.mapTo(mutableSetOf()) { it.id }
                    val enabledSocial = allSocialSources.filter { it.enabled }.sortedBy { it.displayName }
                    val availableSocialIds = enabledSocial.mapTo(mutableSetOf()) { it.id }
                    val selected = if (restored.hasExplicitSourceSelection) {
                        restored.selectedSourceIds.intersect(availableIds)
                    } else if (current.sources.isEmpty()) {
                        availableIds
                    } else {
                        current.selectedSourceIds.intersect(availableIds)
                    }
                    val selectedSocial = if (restored.hasExplicitSocialSourceSelection) {
                        restored.selectedSocialSourceIds.intersect(availableSocialIds)
                    } else if (current.socialSources.isEmpty()) {
                        availableSocialIds
                    } else {
                        current.selectedSocialSourceIds.intersect(availableSocialIds)
                    }
                    current.copy(
                        sources = sources.map { FeedContract.SourceOption(it.id, it.name) },
                        selectedSourceIds = selected,
                        includeReader = restored.includeReader,
                        socialSources = enabledSocial.map {
                            FeedContract.SocialSourceOption(
                                id = it.id,
                                name = it.displayName,
                                host = it.baseUrl.substringAfter("://").substringBefore('/'),
                            )
                        },
                        selectedSocialSourceIds = selectedSocial,
                        includeSocial = restored.includeSocial,
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
            FeedContract.Event.ToggleSocial -> mutateSelection { it.copy(includeSocial = !it.includeSocial) }
            is FeedContract.Event.ToggleSocialSource -> toggleSocialSource(event.sourceId)
            FeedContract.Event.SelectAllSocialSources -> mutateSelection { state ->
                state.copy(selectedSocialSourceIds = state.socialSources.mapTo(mutableSetOf()) { it.id })
            }
            FeedContract.Event.ClearSocialSources -> mutateSelection { it.copy(selectedSocialSourceIds = emptySet()) }
            FeedContract.Event.SelectAllSources -> mutateSelection { state ->
                state.copy(selectedSourceIds = state.sources.mapTo(mutableSetOf()) { it.id })
            }
            FeedContract.Event.ClearForumSources -> mutateSelection { it.copy(selectedSourceIds = emptySet()) }
            FeedContract.Event.Refresh -> refresh()
            FeedContract.Event.LoadOlderSocial -> loadOlderSocial()
            is FeedContract.Event.InteractSocial -> interactSocial(event)
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

    private fun toggleSocialSource(sourceId: String) {
        _state.update { state ->
            val next = state.selectedSocialSourceIds.toMutableSet().apply {
                if (!add(sourceId)) remove(sourceId)
            }
            state.copy(selectedSocialSourceIds = next)
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
                        selectedSocialSourceIds = current.selectedSocialSourceIds,
                        hasExplicitSocialSourceSelection = true,
                        includeSocial = current.includeSocial,
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
                socialSourceIds = current.selectedSocialSourceIds,
                includeSocial = current.includeSocial,
            )
            val failureCount = report.sourceFailures.size + report.readerFailures.size + report.socialFailures.size
            val successCount = report.refreshedSourceIds.size +
                report.refreshedReaderSourceIds.size +
                report.refreshedSocialSourceIds.size
            val message = when {
                report.isSuccess -> getString(Res.string.s_52e6d8f7c0, successCount)
                report.hasAnySuccess -> getString(Res.string.s_b93442c95f, successCount, failureCount)
                else -> getString(Res.string.s_6414520846, failureCount)
            }
            _state.update { it.copy(isRefreshing = false, message = message) }
            _effect.emit(FeedContract.Effect.RefreshPaging)
        }
    }

    private fun loadOlderSocial() {
        val current = _state.value
        if (current.isRefreshing || !current.includeSocial || current.selectedSocialSourceIds.isEmpty()) return
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true, message = null) }
            val report = loadOlderSocialTimeline(current.selectedSocialSourceIds)
            _state.update {
                it.copy(
                    isRefreshing = false,
                    message = if (report.isSuccess) getString(Res.string.s_4d8b4193a3) else getString(Res.string.s_9fbd2eca51),
                )
            }
            _effect.emit(FeedContract.Effect.RefreshPaging)
        }
    }

    private fun interactSocial(event: FeedContract.Event.InteractSocial) {
        screenModelScope.launch {
            val result = interactWithSocialPost(event.post, event.interaction, event.enabled)
            _state.update {
                it.copy(message = result.exceptionOrNull()?.message ?: getString(Res.string.s_f15797df4e))
            }
            if (result.isSuccess) _effect.emit(FeedContract.Effect.RefreshPaging)
        }
    }

    private data class TimelineSelection(
        val forumSourceIds: Set<String>,
        val includeReader: Boolean,
        val socialSourceIds: Set<String>,
        val includeSocial: Boolean,
    )
}
