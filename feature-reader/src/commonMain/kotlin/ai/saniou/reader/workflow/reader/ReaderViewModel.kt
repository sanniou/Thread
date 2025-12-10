package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import ai.saniou.thread.domain.usecase.reader.*
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val getFeedSourcesUseCase: GetFeedSourcesUseCase,
    private val getArticlesUseCase: GetArticlesUseCase,
    private val addFeedSourceUseCase: AddFeedSourceUseCase,
    private val updateFeedSourceUseCase: UpdateFeedSourceUseCase,
    private val deleteFeedSourceUseCase: DeleteFeedSourceUseCase,
    private val markArticleAsReadUseCase: MarkArticleAsReadUseCase,
    private val refreshFeedSourceUseCase: RefreshFeedSourceUseCase,
    private val refreshAllFeedsUseCase: RefreshAllFeedsUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(ReaderContract.State())
    val state: StateFlow<ReaderContract.State> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            getFeedSourcesUseCase().collect { sources ->
                _state.update { it.copy(feedSources = sources) }
            }
        }
        loadArticles(null)
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
        }
    }

    private fun selectFeedSource(id: String?) {
        _state.update { it.copy(selectedFeedSourceId = id) }
        loadArticles(id)
    }

    private fun loadArticles(feedSourceId: String?) {
        val articles = getArticlesUseCase(feedSourceId)
        _state.update { it.copy(articles = articles) }
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
