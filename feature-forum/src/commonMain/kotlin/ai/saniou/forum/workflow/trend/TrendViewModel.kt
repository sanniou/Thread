package ai.saniou.forum.workflow.trend

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.forum.workflow.trend.TrendContract.State
import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.usecase.channel.GetChannelTopicsPagingUseCase
import ai.saniou.thread.domain.usecase.misc.GetTrendUseCase
import app.cash.paging.PagingData
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrendViewModel(
    private val initialSourceId: String,
    private val getTrendUseCase: GetTrendUseCase,
    private val settingsRepository: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val getChannelTopicsPagingUseCase: GetChannelTopicsPagingUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val feedPagingFlow: Flow<PagingData<Topic>> = state
        .map { it.currentSource.id to it.selectedFeedType }
        .distinctUntilChanged()
        .flatMapLatest { (sourceId, feedType) ->
            if (feedType == FeedType.HOT) {
                emptyFlow()
            } else {
                getChannelTopicsPagingUseCase(
                    sourceId = sourceId,
                    channelId = feedType.name.lowercase(),
                    isTimeline = true
                )
            }
        }

    init {
        screenModelScope.launch {
            loadAvailableSources()
            updateCurrentSourceInfo(initialSourceId)
            loadData()
        }
    }

    private fun loadAvailableSources() {
        val sources = sourceRepository.getAvailableSources().map {
            TrendContract.SourceInfo(
                id = it.id,
                name = it.name,
                supportsHistory = it.capabilities.supportsTrendHistory
            )
        }
        _state.update { it.copy(availableSources = sources) }
    }

    private fun updateCurrentSourceInfo(sourceId: String) {
        val source = sourceRepository.getSource(sourceId)
        if (source != null) {
            val availableTypes = mutableListOf<FeedType>()
            if (source.capabilities.supportsTrend) {
                availableTypes.add(FeedType.HOT)
            }
            if (source.capabilities.supportsPagination) {
                availableTypes.add(FeedType.RECOMMEND)
                availableTypes.add(FeedType.CONCERN)
            }

            if (availableTypes.isEmpty()) {
                availableTypes.add(FeedType.HOT) // Fallback
            }

            _state.update {
                it.copy(
                    currentSource = TrendContract.SourceInfo(
                        id = source.id,
                        name = source.name,
                        supportsHistory = source.capabilities.supportsTrendHistory
                    ),
                    availableFeedTypes = availableTypes,
                    selectedFeedType = if (availableTypes.contains(it.selectedFeedType)) it.selectedFeedType else availableTypes.first()
                )
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.Refresh -> loadData(forceRefresh = true)
            Event.PreviousDay -> {
                val currentOffset = state.value.dayOffset
                loadTrend(dayOffset = currentOffset + 1)
            }
            Event.NextDay -> {
                val currentOffset = state.value.dayOffset
                if (currentOffset > 0) {
                    loadTrend(dayOffset = currentOffset - 1)
                }
            }
            is Event.OnTrendItemClick -> {
                screenModelScope.launch {
                    _effect.send(Effect.NavigateToThread(event.topicId))
                }
            }
            Event.OnInfoClick -> {
                screenModelScope.launch {
                    _effect.send(Effect.ShowInfoDialog("https://www.nmbxd1.com/t/50248044"))
                }
            }
            Event.ToggleSource -> {
                // Source switching is handled globally now
            }
            is Event.SelectFeed -> {
                _state.update { it.copy(selectedFeedType = event.feedType) }
                loadData()
            }
            is Event.SelectSource -> {
                updateCurrentSourceInfo(event.sourceId)
                loadData()
            }
        }
    }

    private fun loadData(forceRefresh: Boolean = false) {
        if (_state.value.selectedFeedType == FeedType.HOT) {
            loadTrend(forceRefresh)
        } else {
            // Clear trend items if not HOT
            _state.update { it.copy(items = emptyList()) }
        }
    }

    private fun loadTrend(forceRefresh: Boolean = false, dayOffset: Int = state.value.dayOffset) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, dayOffset = dayOffset) }
            
            val currentSourceId = _state.value.currentSource.id

            getTrendUseCase(currentSourceId, forceRefresh, dayOffset)
                .onSuccess { result ->
                    val (trendDate, items, correctedDayOffset) = result
                    _state.update {
                        it.copy(
                            isLoading = false,
                            trendDate = trendDate.toRelativeTimeString(),
                            items = items.map { item -> item.toUI() },
                            dayOffset = correctedDayOffset ?: it.dayOffset
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.toAppError { loadTrend(forceRefresh = true, dayOffset) }) }
                }
        }
    }
}
