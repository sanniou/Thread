package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.model.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

class GetBookmarksUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(): Flow<List<Bookmark>> = bookmarkRepository.getBookmarks()
}
