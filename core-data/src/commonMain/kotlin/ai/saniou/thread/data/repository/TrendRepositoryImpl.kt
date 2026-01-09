package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TrendRepositoryImpl(
    private val sources: Set<TrendSource> // Injected by DI
) : TrendRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getAvailableTrendSources(): List<TrendSource> {
        return sources.toList()
    }

    override fun getTrendSource(id: String): TrendSource? {
        return sourceMap[id]
    }

    override fun getTrendPagingData(sourceId: String, tab: TrendTab, params: TrendParams): Flow<PagingData<TrendItem>> {
        return getTrendSource(sourceId)?.getTrendPagingData(tab, params) ?: emptyFlow()
    }
}