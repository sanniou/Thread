package ai.saniou.thread.domain.usecase.feed

import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.FeedRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetFeedPagingUseCase(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(sourceId: String, feedType: FeedType): Flow<PagingData<Topic>> {
        return feedRepository.getFeed(sourceId, feedType)
    }
}