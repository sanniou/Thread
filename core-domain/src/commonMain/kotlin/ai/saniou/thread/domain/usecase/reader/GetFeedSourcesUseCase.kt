package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.repository.ReaderRepository
import kotlinx.coroutines.flow.Flow

class GetFeedSourcesUseCase(private val repository: ReaderRepository) {
    operator fun invoke(): Flow<List<FeedSource>> = repository.getAllFeedSources()
}