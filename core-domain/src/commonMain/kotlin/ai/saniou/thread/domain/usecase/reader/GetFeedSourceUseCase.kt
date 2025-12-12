package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.repository.ReaderRepository

class GetFeedSourceUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(id: String): FeedSource? = repository.getFeedSource(id)
}