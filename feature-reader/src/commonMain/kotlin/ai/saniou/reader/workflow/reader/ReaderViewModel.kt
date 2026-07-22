package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.state.toAppError
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.usecase.reader.*
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.usecase.refresh.ObserveRefreshDiagnosticsUseCase
import ai.saniou.thread.domain.model.workspace.ListAnchor
import ai.saniou.thread.domain.usecase.workspace.ObserveWorkspaceSessionUseCase
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.usecase.activity.ExecuteProductActionUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import thread.feature_reader.generated.resources.Res
import thread.feature_reader.generated.resources.export_failed
import thread.feature_reader.generated.resources.import_failed

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ReaderViewModel(
    private val getFeedSourcesUseCase: GetFeedSourcesUseCase,
    private val getArticlesUseCase: GetArticlesUseCase,
    private val addFeedSourceUseCase: AddFeedSourceUseCase,
    private val updateFeedSourceUseCase: UpdateFeedSourceUseCase,
    private val deleteFeedSourceUseCase: DeleteFeedSourceUseCase,
    private val markArticleAsReadUseCase: MarkArticleAsReadUseCase,
    private val executeProductAction: ExecuteProductActionUseCase,
    private val getArticleCountsUseCase: GetArticleCountsUseCase,
    private val observeRefreshDiagnostics: ObserveRefreshDiagnosticsUseCase,
    private val observeScheduler: ObserveReaderSchedulerUseCase,
    observeWorkspaceSession: ObserveWorkspaceSessionUseCase,
    private val updateWorkspaceSession: UpdateWorkspaceSessionUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(ReaderContract.State())
    val state: StateFlow<ReaderContract.State> = _state.asStateFlow()
    val articles: Flow<PagingData<Article>>

    init {
        screenModelScope.launch {
            val restored = observeWorkspaceSession().first().reader
            _state.update {
                it.copy(
                    selectedFeedSourceId = restored.feedSourceId,
                    searchQuery = restored.searchQuery,
                    articleFilter = runCatching { ArticleFilter.valueOf(restored.articleFilter) }
                        .getOrDefault(ArticleFilter.ALL),
                    listAnchor = restored.listAnchor,
                )
            }
        }
        screenModelScope.launch {
            observeScheduler().collect { scheduler ->
                _state.update { it.copy(scheduler = scheduler) }
            }
        }
        screenModelScope.launch {
            observeRefreshDiagnostics().collect { tasks ->
                _state.update { current ->
                    current.copy(
                        refreshFailures = tasks.values
                            .filter { it.status == RefreshStatus.FAILED && it.key.startsWith("reader:") }
                            .sortedByDescending { it.finishedAtEpochMillis },
                    )
                }
            }
        }
        screenModelScope.launch {
            getFeedSourcesUseCase().flatMapLatest { sources ->
                if (sources.isEmpty()) {
                    flowOf(sources to emptyMap())
                } else {
                    combine(sources.map { source ->
                        getArticleCountsUseCase(source.id).map { counts -> source.id to counts }
                    }) { counts -> sources to counts.toMap() }
                }
            }.collect { (sources, counts) ->
                _state.update { current ->
                    val selected = current.selectedFeedSourceId?.takeIf { id -> sources.any { it.id == id } }
                    current.copy(
                        feedSources = sources,
                        articleCounts = counts,
                        selectedFeedSourceId = selected,
                        listAnchor = current.listAnchor?.takeIf { anchor ->
                            anchor.contextKey.startsWith("${selected ?: "all"}:")
                        },
                    )
                }
            }
        }
        val sourceIdFlow = state.map { it.selectedFeedSourceId }.distinctUntilChanged()
        val queryFlow = state.map { it.searchQuery }.distinctUntilChanged().debounce(300L)
        val filterFlow = state.map { it.articleFilter }.distinctUntilChanged()

        articles = combine(sourceIdFlow, queryFlow, filterFlow) { sourceId, query, filter ->
            Triple(sourceId, query, filter)
        }.flatMapLatest { (sourceId, query, filter) ->
            val isRead = if (filter == ArticleFilter.UNREAD) false else null
            val isBookmarked = if (filter == ArticleFilter.BOOKMARKED) true else null
            getArticlesUseCase(sourceId, query, isRead, isBookmarked)
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
            is ReaderContract.Event.OnFilterChanged -> onFilterChanged(event.filter)
            is ReaderContract.Event.OnListPositionChanged -> persistListPosition(event)
            is ReaderContract.Event.OnExportSubscriptions -> exportSubscriptions(event.format)
            is ReaderContract.Event.OnShowImport -> _state.update {
                it.copy(transferDialog = ReaderTransferDialog(event.format, isImport = true))
            }
            is ReaderContract.Event.OnImportSubscriptions -> importSubscriptions(event.payload, event.format)
            ReaderContract.Event.OnDismissTransfer -> _state.update { it.copy(transferDialog = null) }
            ReaderContract.Event.OnMessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        persistReaderState()
    }

    private fun onFilterChanged(filter: ArticleFilter) {
        _state.update { it.copy(articleFilter = filter) }
        persistReaderState()
    }

    private fun selectFeedSource(id: String?) {
        _state.update { it.copy(selectedFeedSourceId = id) }
        persistReaderState()
    }

    private fun persistListPosition(event: ReaderContract.Event.OnListPositionChanged) {
        val anchor = ListAnchor(event.contextKey, event.index, event.offset)
        _state.update { it.copy(listAnchor = anchor) }
        persistReaderState()
    }

    private fun persistReaderState() {
        val current = _state.value
        screenModelScope.launch {
            updateWorkspaceSession { session ->
                session.copy(
                    reader = session.reader.copy(
                        feedSourceId = current.selectedFeedSourceId,
                        articleFilter = current.articleFilter.name,
                        searchQuery = current.searchQuery,
                        listAnchor = current.listAnchor,
                    ),
                    updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
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
                _state.update { it.copy(error = e.toAppError()) }
            }
        }
    }

    private fun deleteSource(id: String) {
        screenModelScope.launch {
            try {
                deleteFeedSourceUseCase(id)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toAppError()) }
            }
        }
    }

    private fun markArticleAsRead(id: String, isRead: Boolean) {
        screenModelScope.launch {
            try {
                markArticleAsReadUseCase(id, isRead)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toAppError()) }
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
                executeProductAction(
                    ProductActionRequest(
                        ProductActionType.REFRESH_SOURCE,
                        sourceId = id,
                        sourceKind = ContentSourceKind.READER,
                    )
                ).getOrThrow()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toAppError()) }
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
                executeProductAction(ProductActionRequest(ProductActionType.REFRESH_ALL_READERS)).getOrThrow()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toAppError()) }
            } finally {
                _state.update { state ->
                    val updatedSources = state.feedSources.map { it.copy(isRefreshing = false) }
                    state.copy(feedSources = updatedSources)
                }
            }
        }
    }

    private fun exportSubscriptions(format: ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat) {
        screenModelScope.launch {
            _state.update { it.copy(isTransferWorking = true) }
            executeProductAction(
                ProductActionRequest(ProductActionType.EXPORT_READER_SUBSCRIPTIONS, readerFormat = format)
            ).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(
                            isTransferWorking = false,
                            transferDialog = ReaderTransferDialog(format, isImport = false, payload = result.output.orEmpty()),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isTransferWorking = false, message = error.message ?: getString(Res.string.export_failed)) }
                },
            )
        }
    }

    private fun importSubscriptions(
        payload: String,
        format: ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat,
    ) {
        screenModelScope.launch {
            _state.update { it.copy(isTransferWorking = true) }
            executeProductAction(
                ProductActionRequest(
                    ProductActionType.IMPORT_READER_SUBSCRIPTIONS,
                    readerFormat = format,
                    payload = payload,
                )
            ).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(
                            isTransferWorking = false,
                            transferDialog = null,
                            message = result.message,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isTransferWorking = false, message = error.message ?: getString(Res.string.import_failed)) }
                },
            )
        }
    }
}
