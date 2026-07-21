package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.feed.FeedRefreshReport
import ai.saniou.thread.domain.model.forum.Topic
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getTimelinePaging(
        sourceIds: Set<String>? = null,
        includeReader: Boolean = true,
        socialSourceIds: Set<String>? = null,
        includeSocial: Boolean = true,
    ): Flow<PagingData<TimelineItem>>

    suspend fun refreshTimeline(
        sourceIds: Set<String>? = null,
        includeReader: Boolean = true,
        socialSourceIds: Set<String>? = null,
        includeSocial: Boolean = true,
    ): FeedRefreshReport

    fun getFeed(sourceId: String, feedType: FeedType): Flow<PagingData<Topic>>
}
