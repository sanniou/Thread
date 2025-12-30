package ai.saniou.thread.domain.usecase.feed

import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.SourceRepository

class GetAggregatedFeedUseCase(
    private val sourceRepository: SourceRepository,
) {
    suspend operator fun invoke(page: Int): Result<List<Topic>> {
        return sourceRepository.getAggregatedFeed(page)
    }
}
