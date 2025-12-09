package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

class GetBookmarksUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(): Flow<List<Bookmark>> = bookmarkRepository.getBookmarks()
}

class AddBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(postId: String, content: String, tag: String? = null) =
        bookmarkRepository.addBookmark(postId, content, tag)
}

class RemoveBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(postId: String) = bookmarkRepository.removeBookmark(postId)
}

class IsBookmarkedUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(postId: String): Flow<Boolean> = bookmarkRepository.isBookmarked(postId)
}