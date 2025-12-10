package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.repository.BookmarkRepository

class AddBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(postId: String, content: String, tag: String? = null) =
        bookmarkRepository.addBookmark(postId, content, tag)
}
