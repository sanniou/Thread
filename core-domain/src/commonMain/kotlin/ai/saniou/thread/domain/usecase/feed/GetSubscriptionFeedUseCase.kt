package ai.saniou.thread.domain.usecase.feed

import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.repository.SourceRepository

class GetAggregatedFeedUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(page: Int): Result<List<Post>> {
        return sourceRepository.getAggregatedFeed(page)
    }
}
