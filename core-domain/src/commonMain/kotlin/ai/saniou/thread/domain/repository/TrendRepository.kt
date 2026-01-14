package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.source.TrendSource
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TrendRepository {
    /**
     * Returns a list of all available trend sources discovered via DI.
     */
    fun getAvailableTrendSources(): List<TrendSource>

    /**
     * Gets a specific trend source by its ID.
     */
    fun getTrendSource(id: String): TrendSource?

    /**
     * A convenience method to directly get the PagingData flow from a source.
     * The ViewModel will primarily use this.
     */
    fun getTrendPagingData(sourceId: String, tab: TrendTab, params: TrendParams): Flow<PagingData<TrendItem>>
}
