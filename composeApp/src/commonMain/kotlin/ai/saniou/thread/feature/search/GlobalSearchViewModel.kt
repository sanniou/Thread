package ai.saniou.thread.feature.search

import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.usecase.search.SearchLocalContentUseCase
import ai.saniou.thread.domain.usecase.workspace.ObserveWorkspaceSessionUseCase
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import ai.saniou.thread.domain.repository.SmartCollectionRepository
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.feature.search.GlobalSearchContract.Event
import ai.saniou.thread.feature.search.GlobalSearchContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModel(
    private val searchLocalContent: SearchLocalContentUseCase,
    observeWorkspaceSession: ObserveWorkspaceSessionUseCase,
    private val updateWorkspaceSession: UpdateWorkspaceSessionUseCase,
    private val smartCollectionRepository: SmartCollectionRepository,
) : ScreenModel {
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val mutableEffects = Channel<GlobalSearchContract.Effect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()
    private var searchJob: Job? = null
    private var session = WorkspaceSession()
    private var restored = false

    val collectionResults: Flow<PagingData<GlobalSearchResult>> = state
        .map { snapshot -> snapshot.activeCollectionId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(PagingData.empty())
            } else {
                smartCollectionRepository.resolvePaging(id)
            }
        }
        .cachedIn(screenModelScope)

    init {
        screenModelScope.launch {
            observeWorkspaceSession().collect { value ->
                session = value
                if (!restored) {
                    restored = true
                    mutableState.update {
                        it.copy(
                            query = value.globalSearchQuery,
                            activeCollectionId = value.activeSmartCollectionId,
                        )
                    }
                    if (value.activeSmartCollectionId == null) scheduleSearch(immediate = true)
                }
            }
        }
        screenModelScope.launch {
            smartCollectionRepository.observeCollections().collect { collections ->
                mutableState.update { current ->
                    current.copy(
                        smartCollections = collections,
                        activeCollectionId = current.activeCollectionId?.takeIf { id -> collections.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.QueryChanged -> {
                mutableState.update { it.copy(query = event.value.take(MAX_QUERY_LENGTH), message = null, activeCollectionId = null) }
                persistSearchState()
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
                mutableState.update { current -> State(smartCollections = current.smartCollections) }
                persistSearchState()
            }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
            is Event.ApplyCollection -> applyCollection(event.id)
        }
    }

    private fun applyCollection(id: String?) {
        searchJob?.cancel()
        if (id == null) {
            mutableState.update { it.copy(activeCollectionId = null, response = null) }
            persistSearchState()
            scheduleSearch(immediate = true)
            return
        }
        val collection = mutableState.value.smartCollections.firstOrNull { it.id == id } ?: return
        mutableState.update {
            it.copy(activeCollectionId = id, query = collection.rules.query, isSearching = false, response = null, message = null)
        }
        persistSearchState()
    }

    fun open(result: ai.saniou.thread.domain.model.search.GlobalSearchResult) {
        screenModelScope.launch { mutableEffects.send(GlobalSearchContract.Effect.OpenResult(result)) }
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        val snapshot = mutableState.value
        if (snapshot.activeCollectionId != null) return
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

    private fun persistSearchState() {
        val snapshot = mutableState.value
        screenModelScope.launch {
            updateWorkspaceSession { current ->
                current.copy(
                    globalSearchQuery = snapshot.query,
                    activeSmartCollectionId = snapshot.activeCollectionId,
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
