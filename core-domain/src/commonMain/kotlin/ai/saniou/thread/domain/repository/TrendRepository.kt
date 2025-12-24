package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Trend

interface TrendRepository {
    suspend fun getTrendItems(forceRefresh: Boolean, dayOffset: Int): Result<Pair<String, List<Trend>>>
}