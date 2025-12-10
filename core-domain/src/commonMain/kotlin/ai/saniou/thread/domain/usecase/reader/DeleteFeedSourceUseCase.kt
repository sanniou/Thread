package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.repository.ReaderRepository

class DeleteFeedSourceUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(id: String) = repository.deleteFeedSource(id)
}