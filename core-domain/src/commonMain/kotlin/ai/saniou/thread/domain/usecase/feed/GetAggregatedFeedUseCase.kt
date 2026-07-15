package ai.saniou.thread.domain.usecase.feed

import ai.saniou.thread.domain.model.feed.AggregatedFeedPage
import ai.saniou.thread.domain.repository.SourceRepository

class GetAggregatedFeedUseCase(
    private val sourceRepository: SourceRepository,
) {
    suspend operator fun invoke(
        page: Int,
        sourceIds: Set<String>? = null,
    ): Result<AggregatedFeedPage> = sourceRepository.getAggregatedFeed(page, sourceIds)
}
