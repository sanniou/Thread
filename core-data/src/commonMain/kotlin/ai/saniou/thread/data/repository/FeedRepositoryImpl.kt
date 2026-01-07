package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.SourceRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FeedRepositoryImpl(
    private val sourceRepository: SourceRepository
) : FeedRepository {
    override fun getTimelinePaging(): Flow<PagingData<TimelineItem>> {
        // TODO: Implement timeline logic
        return emptyFlow()
    }

    override suspend fun refreshTimeline() {
        // TODO: Implement timeline refresh
    }

    override fun getFeed(sourceId: String, feedType: FeedType): Flow<PagingData<Topic>> {
        val source = sourceRepository.getSource(sourceId)
        if (source == null) {
            return emptyFlow()
        }
        return source.getFeedFlow(feedType)
    }
}