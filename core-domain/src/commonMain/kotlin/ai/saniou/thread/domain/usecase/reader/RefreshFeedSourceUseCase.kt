package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.repository.ReaderRepository

class RefreshFeedSourceUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(feedSourceId: String) = repository.refreshFeed(feedSourceId)
}