package ai.saniou.forum.workflow.search

import ai.saniou.forum.workflow.search.SearchContract.Event
import ai.saniou.forum.workflow.search.SearchContract.SearchType
import ai.saniou.forum.workflow.search.SearchContract.State
import ai.saniou.thread.data.source.nmb.NmbSource
import app.cash.paging.cachedIn
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

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: NmbSource,
) : ScreenModel {

    private val _state = MutableStateFlow(State())
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
                _state.update { it.copy(searchType = event.type) }
                // 切换类型时，如果有查询词则重新搜索
                if (_state.value.query.isNotBlank()) {
                    performSearch(_state.value.query)
                }
            }

            Event.ClearQuery -> {
                _state.update {
                    it.copy(
                        query = "",
                        threadPagingData = emptyFlow(),
                        replyPagingData = emptyFlow()
                    )
                }
                searchTrigger.value = ""
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        val currentState = _state.value
        if (currentState.searchType == SearchType.THREAD) {
            val pager = repository.searchTopicsPager(query)
                .cachedIn(screenModelScope)
            _state.update { it.copy(threadPagingData = pager) }
        } else {
            val pager = repository.searchCommentsPager(query)
                .cachedIn(screenModelScope)
            _state.update { it.copy(replyPagingData = pager) }
        }
    }
}
