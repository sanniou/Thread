package ai.saniou.forum.workflow.trend

import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.forum.workflow.trend.TrendContract.State
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.repository.TrendRepository
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
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
import kotlin.time.Clock

class TrendViewModel(
    private val initialSourceId: String,
    private val trendRepository: TrendRepository
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
        }.cachedIn(screenModelScope)

    private fun loadAvailableSources() {
        val sources = trendRepository.getAvailableTrendSources()
        _state.update { it.copy(availableSources = sources) }
    }

    private fun selectSource(sourceId: String) {
        val source = trendRepository.getTrendSource(sourceId) ?: trendRepository.getAvailableTrendSources().firstOrNull()
        if (source != null) {
            val tabs = source.getTrendTabs()
            _state.update {
                it.copy(
                    selectedSource = source,
                    availableTabs = tabs,
                    selectedTab = tabs.firstOrNull(),
                    trendParams = it.trendParams.copy(dayOffset = 0, refreshId = 0) // Reset date and refresh state on source switch
                )
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.SelectSource -> {
                selectSource(event.sourceId)
            }

            is Event.SelectTab -> {
                val tab = _state.value.availableTabs.find { it.id == event.tabId }
                if (tab != null) {
                    // Reset refreshId when switching tabs to use cache by default
                    _state.update {
                        it.copy(
                            selectedTab = tab,
                            trendParams = it.trendParams.copy(refreshId = 0)
                        )
                    }
                }
            }

            is Event.SelectDate -> {
                _state.update {
                    // Reset refreshId when changing date to use cache by default
                    it.copy(trendParams = it.trendParams.copy(dayOffset = event.dayOffset, refreshId = 0))
                }
            }

            is Event.OnTrendItemClick -> {
                screenModelScope.launch {
                    _effect.send(Effect.NavigateToThread(event.item.id, event.item.sourceId))
                }
            }

            Event.Refresh -> {
                _state.update {
                    // Use current timestamp as refreshId to force a new Pager creation with NETWORK_ONLY policy
                    it.copy(trendParams = it.trendParams.copy(refreshId = Clock.System.now().toEpochMilliseconds()))
                }
            }
        }
    }
}
