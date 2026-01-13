package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab

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
     * Fetches trend data for a given tab and page.
     * This is used by the RemoteMediator to fetch data from the network.
     */
    suspend fun fetchTrendData(
        tab: TrendTab,
        params: TrendParams,
        page: Int,
    ): Result<List<TrendItem>>

    fun trendDataEnded(tab: TrendTab, params: TrendParams, trends: List<TrendItem>): Boolean
}
