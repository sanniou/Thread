package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.forum.Topic
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getTimelinePaging(): Flow<PagingData<TimelineItem>>
    suspend fun refreshTimeline()

    fun getFeed(sourceId: String, feedType: FeedType): Flow<PagingData<Topic>>
}
