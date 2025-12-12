package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.repository.ReaderRepository

class ToggleArticleBookmarkUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(id: String, currentStatus: Boolean) {
        repository.markArticleAsBookmarked(id, !currentStatus)
    }
}