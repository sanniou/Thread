package ai.saniou.forum.workflow.search

import ai.saniou.forum.workflow.search.SearchContract.Event
import ai.saniou.forum.workflow.search.SearchContract.SearchType
import ai.saniou.forum.workflow.search.SearchContract.State
import ai.saniou.thread.domain.repository.ForumSearchRepository
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchViewModelParams(
    val sourceId: String,
    val channelId: String? = null,
    val channelName: String? = null,
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val sourceId: String,
    private val repository: ForumSearchRepository,
    private val channelId: String? = null,
    private val channelName: String? = null,
) : ScreenModel {

    /** When set, search is scoped to a forum via [ForumSearchRepository.searchChannelTopics]. */
    val isChannelScoped: Boolean =
        !channelId.isNullOrBlank() || !channelName.isNullOrBlank()

    private val _state = MutableStateFlow(
        State(searchType = if (isChannelScoped) SearchType.THREAD else SearchType.THREAD),
    )
    val state = _state.asStateFlow()

    private val searchTrigger = MutableStateFlow("")

    init {
        screenModelScope.launch {
            searchTrigger
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                searchTrigger.value = event.query
            }

            is Event.TypeChanged -> {
                if (isChannelScoped) return
                _state.update { it.copy(searchType = event.type) }
                if (_state.value.query.isNotBlank()) {
                    performSearch(_state.value.query)
                }
            }

            Event.ClearQuery -> {
                _state.update {
                    it.copy(
                        query = "",
                        threadPagingData = emptyFlow(),
                        replyPagingData = emptyFlow(),
                        channelPagingData = emptyFlow(),
                        userPagingData = emptyFlow(),
                    )
                }
                searchTrigger.value = ""
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        if (isChannelScoped) {
            val pager = repository.searchChannelTopics(
                sourceId = sourceId,
                channelId = channelId.orEmpty(),
                channelName = channelName.orEmpty().ifBlank { channelId.orEmpty() },
                query = query,
            ).cachedIn(screenModelScope)
            _state.update { it.copy(searchType = SearchType.THREAD, threadPagingData = pager) }
            return
        }
        when (_state.value.searchType) {
            SearchType.THREAD -> {
                val pager = repository.searchTopics(sourceId, query).cachedIn(screenModelScope)
                _state.update { it.copy(threadPagingData = pager) }
            }
            SearchType.REPLY -> {
                val pager = repository.searchComments(sourceId, query).cachedIn(screenModelScope)
                _state.update { it.copy(replyPagingData = pager) }
            }
            SearchType.CHANNEL -> {
                val pager = repository.searchChannels(sourceId, query).cachedIn(screenModelScope)
                _state.update { it.copy(channelPagingData = pager) }
            }
            SearchType.USER -> {
                val pager = repository.searchUsers(sourceId, query).cachedIn(screenModelScope)
                _state.update { it.copy(userPagingData = pager) }
            }
        }
    }
}
