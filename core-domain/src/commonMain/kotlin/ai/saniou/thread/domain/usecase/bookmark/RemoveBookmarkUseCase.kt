package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.repository.BookmarkRepository

class RemoveBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(postId: String) = bookmarkRepository.removeBookmark(postId)
}
