package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.repository.ReaderRepository

class MarkArticleAsReadUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(id: String, isRead: Boolean) = repository.markArticleAsRead(id, isRead)
}