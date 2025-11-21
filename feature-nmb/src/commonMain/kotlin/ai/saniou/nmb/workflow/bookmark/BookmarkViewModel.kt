package ai.saniou.nmb.workflow.bookmark

import ai.saniou.nmb.db.Database
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.Effect
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.Event
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.State
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val db: Database
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        onEvent(Event.LoadBookmarks)
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadBookmarks -> loadBookmarks()
            is Event.DeleteBookmark -> deleteBookmark(event.id)
        }
    }

    private fun loadBookmarks() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            db.bookmarkQueries.selectAll()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = "加载收藏失败: ${e.message}") }
                }
                .collectLatest { bookmarks ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            bookmarks = bookmarks
                        )
                    }
                }
        }
    }

    private fun deleteBookmark(id: String) {
        screenModelScope.launch {
            db.bookmarkQueries.delete(id)
            _effect.send(Effect.ShowSnackbar("已取消收藏"))
        }
    }
}
