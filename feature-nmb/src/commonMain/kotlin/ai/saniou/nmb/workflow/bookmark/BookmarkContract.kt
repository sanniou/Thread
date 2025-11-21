package ai.saniou.nmb.workflow.bookmark

import ai.saniou.nmb.db.table.Bookmark

interface BookmarkContract {
    data class State(
        val bookmarks: List<Bookmark> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )

    sealed interface Event {
        object LoadBookmarks : Event
        data class DeleteBookmark(val id: String) : Event
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
    }
}
