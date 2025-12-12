package ai.saniou.forum.workflow.bookmark

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.bookmark.Tag
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface BookmarkContract {
    data class State(
        val bookmarks: Flow<PagingData<Bookmark>> = emptyFlow(),
        val searchQuery: String = "",
        val selectedTags: List<Tag> = emptyList(),
        val allTags: List<Tag> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    sealed interface Event {
        data class OnSearchQueryChanged(val query: String) : Event
        data class OnTagSelected(val tag: Tag) : Event
        data class OnTagDeselected(val tag: Tag) : Event
        data class DeleteBookmark(val bookmark: Bookmark) : Event
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
    }
}
