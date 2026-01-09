package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.model.TrendParams
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface TrendSource {
    /**
     * The unique identifier for the source (e.g., "nmb", "tieba").
     * This should match the parent Source's ID.
     */
    val id: String

    /**
     * The display name for the source (e.g., "A岛", "贴吧").
     */
    val name: String

    /**
     * Returns a list of supported trend tabs for this source.
     * This will be called to dynamically build the UI.
     */
    fun getTrendTabs(): List<TrendTab>

    /**
     * Returns a unified PagingData flow for a given tab.
     * All source-specific logic (API calls, parsing, caching) is encapsulated here.
     */
    fun getTrendPagingData(tab: TrendTab, params: TrendParams): Flow<PagingData<TrendItem>>
}