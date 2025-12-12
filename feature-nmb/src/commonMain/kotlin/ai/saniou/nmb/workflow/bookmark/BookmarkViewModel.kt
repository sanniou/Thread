package ai.saniou.nmb.workflow.bookmark

import ai.saniou.nmb.workflow.bookmark.BookmarkContract.Effect
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.Event
import ai.saniou.nmb.workflow.bookmark.BookmarkContract.State
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.usecase.bookmark.GetBookmarksUseCase
import ai.saniou.thread.domain.usecase.bookmark.GetTagsUseCase
import ai.saniou.thread.domain.usecase.bookmark.RemoveBookmarkUseCase
import androidx.compose.runtime.snapshotFlow
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BookmarkViewModel(
    private val getBookmarksUseCase: GetBookmarksUseCase,
    private val removeBookmarkUseCase: RemoveBookmarkUseCase,
    private val getTagsUseCase: GetTagsUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        screenModelScope.launch {
            snapshotFlow { _state.value.searchQuery to _state.value.selectedTags }
                .debounce(300)
                .flatMapLatest { (query, tags) ->
                    getBookmarksUseCase(query, tags.map { it.name }).cachedIn(screenModelScope)
                }.let { pagingData ->
                    _state.update { it.copy(bookmarks = pagingData) }
                }
        }
        screenModelScope.launch {
            getTagsUseCase().collect { tags ->
                _state.update { it.copy(allTags = tags) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnSearchQueryChanged -> _state.update { it.copy(searchQuery = event.query) }
            is Event.OnTagSelected -> _state.update { it.copy(selectedTags = it.selectedTags + event.tag) }
            is Event.OnTagDeselected -> _state.update { it.copy(selectedTags = it.selectedTags - event.tag) }
            is Event.DeleteBookmark -> deleteBookmark(event.bookmark)
        }
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        screenModelScope.launch {
            removeBookmarkUseCase(bookmark.id)
            _effect.send(Effect.ShowSnackbar("已取消收藏"))
        }
    }
}
