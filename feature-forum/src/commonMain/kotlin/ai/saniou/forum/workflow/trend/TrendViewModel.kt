package ai.saniou.forum.workflow.trend

import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.forum.workflow.trend.TrendContract.State
import ai.saniou.thread.domain.usecase.misc.GetTrendUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrendViewModel(
    private val getTrendUseCase: GetTrendUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadTrend()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.Refresh -> loadTrend(forceRefresh = true)
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
                    _effect.send(Effect.NavigateToThread(event.threadId))
                }
            }
            Event.OnInfoClick -> {
                 screenModelScope.launch {
                    _effect.send(Effect.ShowInfoDialog("https://www.nmbxd1.com/t/50248044"))
                }
            }
        }
    }

    private fun loadTrend(forceRefresh: Boolean = false, dayOffset: Int = state.value.dayOffset) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, dayOffset = dayOffset) }

            getTrendUseCase(forceRefresh, dayOffset)
                .onSuccess { (trendDate, items) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            trendDate = trendDate,
                            items = items.map { it.toUI() }
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.toAppError { loadTrend(forceRefresh = true, dayOffset) }) }
                }
        }
    }
}
