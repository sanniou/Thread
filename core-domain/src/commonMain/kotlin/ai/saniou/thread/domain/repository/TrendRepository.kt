package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.TrendResult

interface TrendRepository {
    suspend fun getTrendItems(forceRefresh: Boolean, dayOffset: Int): Result<TrendResult>
}