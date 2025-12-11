package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import ai.saniou.thread.domain.usecase.reader.*
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val getFeedSourcesUseCase: GetFeedSourcesUseCase,
    private val getArticlesUseCase: GetArticlesUseCase,
    private val addFeedSourceUseCase: AddFeedSourceUseCase,
    private val updateFeedSourceUseCase: UpdateFeedSourceUseCase,
    private val deleteFeedSourceUseCase: DeleteFeedSourceUseCase,
    private val markArticleAsReadUseCase: MarkArticleAsReadUseCase,
    private val refreshFeedSourceUseCase: RefreshFeedSourceUseCase,
    private val refreshAllFeedsUseCase: RefreshAllFeedsUseCase,
    private val getArticleCountsUseCase: GetArticleCountsUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(ReaderContract.State())
    val state: StateFlow<ReaderContract.State> = _state.asStateFlow()
    val articles: Flow<PagingData<Article>>

    init {
        screenModelScope.launch {
            getFeedSourcesUseCase().collect { sources ->
                _state.update { it.copy(feedSources = sources) }
                sources.forEach { source ->
                    launch {
                        getArticleCountsUseCase(source.id).collect { counts ->
                            _state.update {
                                val newCounts = it.articleCounts.toMutableMap()
                                newCounts[source.id] = counts
                                it.copy(articleCounts = newCounts)
                            }
                        }
                    }
                }
            }
        }
        val sourceIdFlow = state.map { it.selectedFeedSourceId }.distinctUntilChanged()
        // 防抖处理，避免用户输入时频繁请求
        val queryFlow = state.map { it.searchQuery }.distinctUntilChanged().debounce(300L)

        articles = combine(sourceIdFlow, queryFlow) { sourceId, query ->
            sourceId to query
        }.flatMapLatest { (sourceId, query) ->
            getArticlesUseCase(sourceId, query)
        }.cachedIn(screenModelScope)
    }

    fun onEvent(event: ReaderContract.Event) {
        when (event) {
            is ReaderContract.Event.OnSelectFeedSource -> selectFeedSource(event.id)
            is ReaderContract.Event.OnRefreshFeedSource -> refreshFeedSource(event.id)
            ReaderContract.Event.OnRefreshAll -> refreshAll()
            ReaderContract.Event.OnShowAddDialog -> showDialog(null)
            is ReaderContract.Event.OnShowEditDialog -> showDialog(event.source)
            ReaderContract.Event.OnDismissDialog -> dismissDialog()
            is ReaderContract.Event.OnSaveSource -> saveSource(event.source)
            is ReaderContract.Event.OnDeleteSource -> deleteSource(event.id)
            is ReaderContract.Event.OnMarkArticleAsRead -> markArticleAsRead(event.id, event.isRead)
            is ReaderContract.Event.OnSearchQueryChanged -> onSearchQueryChanged(event.query)
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    private fun selectFeedSource(id: String?) {
        _state.update { it.copy(selectedFeedSourceId = id) }
    }

    private fun showDialog(source: FeedSource?) {
        _state.update { it.copy(isDialogShown = true, editingSource = source) }
    }

    private fun dismissDialog() {
        _state.update { it.copy(isDialogShown = false, editingSource = null) }
    }

    private fun saveSource(source: FeedSource) {
        screenModelScope.launch {
            try {
                if (_state.value.editingSource == null) {
                    addFeedSourceUseCase(source)
                } else {
                    updateFeedSourceUseCase(source)
                }
                dismissDialog()
                refreshFeedSource(source.id)
            } catch (e: Exception) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    private fun deleteSource(id: String) {
        screenModelScope.launch {
            try {
                deleteFeedSourceUseCase(id)
            } catch (e: Exception) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    private fun markArticleAsRead(id: String, isRead: Boolean) {
        screenModelScope.launch {
            try {
                markArticleAsReadUseCase(id, isRead)
            } catch (e: Exception) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    private fun refreshFeedSource(id: String) {
        screenModelScope.launch {
            _state.update { state ->
                val updatedSources = state.feedSources.map { if (it.id == id) it.copy(isRefreshing = true) else it }
                state.copy(feedSources = updatedSources)
            }
            try {
                refreshFeedSourceUseCase(id)
            } catch (e: Exception) {
                _state.update { it.copy(error = e) }
            } finally {
                _state.update { state ->
                    val updatedSources = state.feedSources.map { if (it.id == id) it.copy(isRefreshing = false) else it }
                    state.copy(feedSources = updatedSources)
                }
            }
        }
    }

    private fun refreshAll() {
        screenModelScope.launch {
            _state.update { state ->
                val updatedSources = state.feedSources.map { it.copy(isRefreshing = true) }
                state.copy(feedSources = updatedSources)
            }
            try {
                refreshAllFeedsUseCase()
            } catch (e: Exception) {
                _state.update { it.copy(error = e) }
            } finally {
                _state.update { state ->
                    val updatedSources = state.feedSources.map { it.copy(isRefreshing = false) }
                    state.copy(feedSources = updatedSources)
                }
            }
        }
    }
}
