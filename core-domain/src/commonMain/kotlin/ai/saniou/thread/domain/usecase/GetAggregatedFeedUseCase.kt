package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.FeedRepository

class GetAggregatedFeedUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(page: Int): Result<List<Post>> {
        return feedRepository.getAggregatedFeed(page)
    }
}