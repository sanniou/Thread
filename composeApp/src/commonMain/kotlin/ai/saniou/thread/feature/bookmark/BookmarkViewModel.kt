package ai.saniou.thread.feature.bookmark

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.usecase.bookmark.GetBookmarksUseCase
import ai.saniou.thread.domain.usecase.bookmark.GetTagsUseCase
import ai.saniou.thread.domain.usecase.bookmark.RemoveBookmarkUseCase
import ai.saniou.thread.feature.bookmark.BookmarkContract.Effect
import ai.saniou.thread.feature.bookmark.BookmarkContract.Event
import ai.saniou.thread.feature.bookmark.BookmarkContract.State
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_0fc87e8309
import thread.composeapp.generated.resources.s_25a55e1483

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

    val bookmarksFlow = combine(
        _state.map { it.searchQuery }.distinctUntilChanged(),
        _state.map { it.selectedTags }.distinctUntilChanged()
    ) { query, tags ->
        query to tags
    }.debounce(300)
    .flatMapLatest { (query, tags) ->
        getBookmarksUseCase(query, tags.map { it.name })
    }.cachedIn(screenModelScope)


    init {
        screenModelScope.launch {
            getTagsUseCase().collect { tags ->
                _state.update { it.copy(allTags = tags) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnSearchQueryChanged -> _state.update { it.copy(searchQuery = event.query) }
            is Event.OnTagSelected -> _state.update {
                if (event.tag in it.selectedTags) it else it.copy(selectedTags = it.selectedTags + event.tag)
            }
            is Event.OnTagDeselected -> _state.update { it.copy(selectedTags = it.selectedTags - event.tag) }
            is Event.DeleteBookmark -> deleteBookmark(event.bookmark)
            Event.ToggleSelectionMode -> _state.update {
                it.copy(
                    isSelectionMode = !it.isSelectionMode,
                    selectedBookmarks = emptySet() // Clear selection when toggling
                )
            }
            is Event.ToggleBookmarkSelection -> _state.update {
                val currentSelection = it.selectedBookmarks
                val newSelection = if (currentSelection.contains(event.bookmarkId)) {
                    currentSelection - event.bookmarkId
                } else {
                    currentSelection + event.bookmarkId
                }
                it.copy(selectedBookmarks = newSelection)
            }
            Event.DeleteSelectedBookmarks -> deleteSelectedBookmarks()
        }
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        screenModelScope.launch {
            removeBookmarkUseCase(bookmark.id)
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_0fc87e8309)))
        }
    }

    private fun deleteSelectedBookmarks() {
        val selectedIds = _state.value.selectedBookmarks
        if (selectedIds.isEmpty()) return

        screenModelScope.launch {
            selectedIds.forEach { id ->
                removeBookmarkUseCase(id)
            }
            _effect.send(Effect.ShowSnackbar(getString(Res.string.s_25a55e1483, selectedIds.size)))
            _state.update { it.copy(isSelectionMode = false, selectedBookmarks = emptySet()) }
        }
    }
}
