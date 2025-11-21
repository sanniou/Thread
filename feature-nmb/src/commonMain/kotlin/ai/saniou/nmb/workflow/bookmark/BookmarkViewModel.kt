package ai.saniou.nmb.workflow.bookmark

import ai.saniou.nmb.domain.GetBookmarksUseCase
import ai.saniou.nmb.domain.RemoveBookmarkUseCase
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.Effect
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.Event
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val removeBookmarkUseCase: RemoveBookmarkUseCase
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
            getBookmarksUseCase()
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
            removeBookmarkUseCase(id)
            _effect.send(Effect.ShowSnackbar("已取消收藏"))
        }
    }
}