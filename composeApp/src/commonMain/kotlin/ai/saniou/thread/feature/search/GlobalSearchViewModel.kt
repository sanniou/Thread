package ai.saniou.thread.feature.search

import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.usecase.search.SearchLocalContentUseCase
import ai.saniou.thread.domain.usecase.workspace.ObserveWorkspaceSessionUseCase
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import ai.saniou.thread.feature.search.GlobalSearchContract.Event
import ai.saniou.thread.feature.search.GlobalSearchContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class GlobalSearchViewModel(
    private val searchLocalContent: SearchLocalContentUseCase,
    observeWorkspaceSession: ObserveWorkspaceSessionUseCase,
    private val updateWorkspaceSession: UpdateWorkspaceSessionUseCase,
) : ScreenModel {
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val mutableEffects = Channel<GlobalSearchContract.Effect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()
    private var searchJob: Job? = null
    private var session = WorkspaceSession()
    private var restored = false

    init {
        screenModelScope.launch {
            observeWorkspaceSession().collect { value ->
                session = value
                if (!restored) {
                    restored = true
                    mutableState.update { it.copy(query = value.globalSearchQuery) }
                    scheduleSearch(immediate = true)
                }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.QueryChanged -> {
                mutableState.update { it.copy(query = event.value.take(MAX_QUERY_LENGTH), message = null) }
                persistQuery()
                scheduleSearch()
            }
            is Event.TypeToggled -> {
                mutableState.update { current ->
                    val next = if (event.type in current.selectedTypes) {
                        current.selectedTypes - event.type
                    } else {
                        current.selectedTypes + event.type
                    }
                    current.copy(selectedTypes = next)
                }
                scheduleSearch(immediate = true)
            }
            Event.Retry -> scheduleSearch(immediate = true)
            Event.Clear -> {
                searchJob?.cancel()
                mutableState.value = State()
                persistQuery()
            }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
        }
    }

    fun open(result: ai.saniou.thread.domain.model.search.GlobalSearchResult) {
        screenModelScope.launch { mutableEffects.send(GlobalSearchContract.Effect.OpenResult(result)) }
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        val snapshot = mutableState.value
        if (snapshot.query.trim().length < MIN_QUERY_LENGTH || snapshot.selectedTypes.isEmpty()) {
            mutableState.update { it.copy(response = null, isSearching = false) }
            return
        }
        searchJob = screenModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MILLIS)
            val current = mutableState.value
            mutableState.update { it.copy(isSearching = true, message = null) }
            runCatching { searchLocalContent(current.query, current.selectedTypes) }.fold(
                onSuccess = { response -> mutableState.update { it.copy(response = response, isSearching = false) } },
                onFailure = { error ->
                    mutableState.update {
                        it.copy(isSearching = false, message = error.message ?: "本地搜索失败")
                    }
                },
            )
        }
    }

    private fun persistQuery() {
        val query = mutableState.value.query
        screenModelScope.launch {
            updateWorkspaceSession { current ->
                current.copy(
                    globalSearchQuery = query,
                    updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val MAX_QUERY_LENGTH = 240
        const val SEARCH_DEBOUNCE_MILLIS = 260L
    }
}
