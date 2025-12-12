package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository

class AddBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(bookmark: Bookmark) = bookmarkRepository.addBookmark(bookmark)
}