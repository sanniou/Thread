package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.feed.TimelineItem
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getTimelinePaging(): Flow<PagingData<TimelineItem>>
    suspend fun refreshTimeline()
}