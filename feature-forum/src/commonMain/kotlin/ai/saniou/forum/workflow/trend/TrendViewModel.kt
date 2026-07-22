package ai.saniou.forum.workflow.trend

import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.forum.workflow.trend.TrendContract.State
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.usecase.post.SubmitNotInterestedUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class TrendViewModel(
    private val initialSourceId: String,
    private val trendRepository: TrendRepository,
    private val submitNotInterestedUseCase: SubmitNotInterestedUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        screenModelScope.launch {
            loadAvailableSources()
            selectSource(initialSourceId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val trendPagingFlow: Flow<PagingData<TrendItem>> = state
        .map { Triple(it.selectedSource, it.selectedTab, it.trendParams) }
        .distinctUntilChanged()
        .flatMapLatest { (source, tab, params) ->
            if (source == null || tab == null) {
                emptyFlow()
            } else {
                trendRepository.getTrendPagingData(source.id, tab, params)
            }
        }
        .combine(state.map { it.dismissedTopicIds }.distinctUntilChanged()) { paging, dismissed ->
            if (dismissed.isEmpty()) paging
            else paging.filter { it.topicId !in dismissed }
        }
        .cachedIn(screenModelScope)

    private fun loadAvailableSources() {
        val sources = trendRepository.getAvailableTrendSources()
        _state.update { it.copy(availableSources = sources) }
    }

    private fun selectSource(sourceId: String) {
        val source = trendRepository.getTrendSource(sourceId)
            ?: trendRepository.getAvailableTrendSources().firstOrNull()
        if (source != null) {
            val tabs = source.getTrendTabs()
            _state.update {
                it.copy(
                    selectedSource = source,
                    availableTabs = tabs,
                    selectedTab = tabs.firstOrNull(),
                    trendParams = it.trendParams.copy(dayOffset = 0, refreshId = 0),
                    dismissedTopicIds = emptySet(),
                    notInterestedInFlight = emptySet(),
                )
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.SelectSource -> selectSource(event.sourceId)

            is Event.SelectTab -> {
                val tab = _state.value.availableTabs.find { it.id == event.tabId }
                if (tab != null) {
                    _state.update {
                        it.copy(
                            selectedTab = tab,
                            trendParams = it.trendParams.copy(refreshId = 0),
                            dismissedTopicIds = emptySet(),
                            notInterestedInFlight = emptySet(),
                        )
                    }
                }
            }

            is Event.SelectDate -> {
                _state.update {
                    it.copy(
                        trendParams = it.trendParams.copy(
                            dayOffset = event.dayOffset,
                            refreshId = 0,
                        ),
                    )
                }
            }

            is Event.OnTrendItemClick -> {
                screenModelScope.launch {
                    _effect.send(Effect.NavigateToThread(event.item.topicId, event.item.sourceId))
                }
            }

            Event.Refresh -> {
                _state.update {
                    it.copy(
                        trendParams = it.trendParams.copy(
                            refreshId = Clock.System.now().toEpochMilliseconds(),
                        ),
                        dismissedTopicIds = emptySet(),
                        notInterestedInFlight = emptySet(),
                    )
                }
            }

            is Event.NotInterested -> handleNotInterested(event.item)
        }
    }

    private fun handleNotInterested(item: TrendItem) {
        val tab = _state.value.selectedTab
        if (tab?.supportsNotInterested != true) {
            screenModelScope.launch {
                _effect.send(Effect.ShowSnackbar("当前页签不支持「不感兴趣」"))
            }
            return
        }
        if (item.topicId in _state.value.notInterestedInFlight ||
            item.topicId in _state.value.dismissedTopicIds
        ) {
            return
        }
        _state.update { it.copy(notInterestedInFlight = it.notInterestedInFlight + item.topicId) }
        screenModelScope.launch {
            val channelId = item.payload["channelId"] as? String
            val result = submitNotInterestedUseCase(
                sourceId = item.sourceId,
                topicId = item.topicId,
                channelId = channelId,
            )
            _state.update {
                it.copy(notInterestedInFlight = it.notInterestedInFlight - item.topicId)
            }
            result.fold(
                onSuccess = { msg ->
                    _state.update {
                        it.copy(dismissedTopicIds = it.dismissedTopicIds + item.topicId)
                    }
                    _effect.send(Effect.ShowSnackbar(msg.ifBlank { "已标记不感兴趣" }))
                },
                onFailure = { e ->
                    _effect.send(
                        Effect.ShowSnackbar(e.message?.takeIf(String::isNotBlank) ?: "标记不感兴趣失败"),
                    )
                },
            )
        }
    }
}
