package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TrendResult
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TrendRepository {
    suspend fun getTrendItems(sourceId: String, forceRefresh: Boolean, dayOffset: Int): Result<TrendResult>

    fun getHotThreads(): Flow<PagingData<Topic>>

    fun getTopicList(): Flow<PagingData<Topic>>

    fun getConcernFeed(): Flow<PagingData<Topic>>

    fun getPersonalizedFeed(): Flow<PagingData<Topic>>
}