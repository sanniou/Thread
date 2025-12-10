package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

class IsBookmarkedUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(id: String): Flow<Boolean> = bookmarkRepository.isBookmarked(id)
}