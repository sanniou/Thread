package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.state.AppError
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.ReaderSchedulerState
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import ai.saniou.thread.domain.refresh.RefreshTaskState
import ai.saniou.thread.domain.model.workspace.ListAnchor

enum class ArticleFilter {
    ALL, UNREAD, BOOKMARKED
}

data class ReaderTransferDialog(
    val format: ReaderSubscriptionFormat,
    val isImport: Boolean,
    val payload: String = "",
)

interface ReaderContract {
    data class State(
        val feedSources: List<FeedSource> = emptyList(),
        val selectedFeedSourceId: String? = null,
        val error: AppError? = null,
        val isDialogShown: Boolean = false,
        val editingSource: FeedSource? = null,
        val articleCounts: Map<String, Pair<Int, Int>> = emptyMap(),
        val searchQuery: String = "",
        val articleFilter: ArticleFilter = ArticleFilter.ALL,
        val refreshFailures: List<RefreshTaskState> = emptyList(),
        val scheduler: ReaderSchedulerState = ReaderSchedulerState(),
        val transferDialog: ReaderTransferDialog? = null,
        val isTransferWorking: Boolean = false,
        val message: String? = null,
        val listAnchor: ListAnchor? = null,
    )

    sealed interface Event {
        data class OnSelectFeedSource(val id: String?) : Event
        object OnShowAddDialog : Event
        data class OnShowEditDialog(val source: FeedSource) : Event
        object OnDismissDialog : Event
        data class OnSaveSource(val source: FeedSource) : Event
        data class OnDeleteSource(val id: String) : Event
        data class OnRefreshFeedSource(val id: String) : Event
        object OnRefreshAll : Event
        data class OnMarkArticleAsRead(val id: String, val isRead: Boolean) : Event
        data class OnSearchQueryChanged(val query: String) : Event
        data class OnFilterChanged(val filter: ArticleFilter) : Event
        data class OnListPositionChanged(val contextKey: String, val index: Int, val offset: Int) : Event
        data class OnExportSubscriptions(val format: ReaderSubscriptionFormat) : Event
        data class OnShowImport(val format: ReaderSubscriptionFormat) : Event
        data class OnImportSubscriptions(val payload: String, val format: ReaderSubscriptionFormat) : Event
        object OnDismissTransfer : Event
        object OnMessageShown : Event
    }
}
