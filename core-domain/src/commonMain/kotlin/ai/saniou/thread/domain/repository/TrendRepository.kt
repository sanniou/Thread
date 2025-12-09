package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.Trend

interface TrendRepository {
    suspend fun getTrendItems(forceRefresh: Boolean): Result<Pair<String, List<Trend>>>
}