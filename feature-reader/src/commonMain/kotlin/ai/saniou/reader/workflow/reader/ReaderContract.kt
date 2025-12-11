package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import kotlinx.coroutines.flow.Flow

interface ReaderContract {
    data class State(
        val feedSources: List<FeedSource> = emptyList(),
        val selectedFeedSourceId: String? = null,
        val isLoading: Boolean = false,
        val error: Throwable? = null,
        val isDialogShown: Boolean = false,
        val editingSource: FeedSource? = null,
        val searchQuery: String = ""
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
    }
}